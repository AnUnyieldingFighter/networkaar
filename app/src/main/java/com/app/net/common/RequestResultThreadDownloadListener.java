package com.app.net.common;

import android.os.Handler;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;

import com.retrofits.net.manager.BaseManager;
import com.retrofits.net.manager.TaskResultThreadListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * 下载
 * Created by Administrator on 2017/6/14.
 */

public class RequestResultThreadDownloadListener extends TaskResultThreadListener<ResponseBody> {
    public static final String TEMP_FILE_SUFFIX = ".l";
    public static final String RESUME_METADATA_SUFFIX = ".l.meta";

    private String url;
    private String filePath;
    // false 表示普通下载：不追加临时文件，也不保存断点续传校验信息。
    private boolean resumeEnabled = false;

    public RequestResultThreadDownloadListener(BaseManager baseManager, Call<ResponseBody> call) {
        super(call);
        this.baseManager = baseManager;
    }

    public RequestResultThreadDownloadListener(BaseManager baseManager, Call<ResponseBody> call, Object reqObj) {
        super(call, reqObj);
        this.baseManager = baseManager;
    }

    public RequestResultThreadDownloadListener(BaseManager baseManager, Call<ResponseBody> call, String other, Object reqObj) {
        super(call, other, reqObj);
        this.baseManager = baseManager;
    }


    public void setDownloadFile(String url, String filePath) {
        this.url = url;
        this.filePath = filePath;
    }

    public void setResumeEnabled(boolean resumeEnabled) {
        this.resumeEnabled = resumeEnabled;
    }

    //1正在下载 2:下载完成
    private volatile int downloadType;
    //true 停止下载
    private volatile boolean isStop;
    private volatile boolean stopNotified;

    public void onStopDownload() {
        onStop();
    }

    //停止下载文件
    @Override
    protected void onStop() {
        if (isStop || downloadType == 2) {
            return;
        }
        isStop = true;
        //先标记停止再取消 Call，避免关闭流产生的异常被误报为下载失败。
        super.onStop();
        // 停止回调由下载线程或 onFailure 统一发送，避免取消 Call 时重复回调停止。
    }

    private void onStopListener() {
        if (stopNotified) {
            return;
        }
        stopNotified = true;
        BaseManager.Progress listener = baseManager.getProgress();
        Handler h = baseManager.getHandleCall();
        //1：开始 2：进行中 3：完成 4：出错 5 停止
        onBack(h, listener, 5, url, filePath, currentLength, totalLength, "stop");

    }

    @Override
    public void onRequestResult(Call<ResponseBody> call, Response<ResponseBody> response) {
        onDownloadFile(response);
    }

    volatile long currentLength = 0;
    volatile long totalLength = 0;
    private long lastProgressTime;

    @Override
    public void onFailure(Call<ResponseBody> call, Throwable throwable) {
        downloadType = 2;
        if (isStop || call.isCanceled()) {
            // 主动取消已经发送停止事件，不能再向业务层重复报告网络错误。
            onStopListener();
            return;
        }
        String message = throwable == null ? "下载失败" : throwable.getMessage();
        onBack(baseManager.getHandleCall(), baseManager.getProgress(false),
                4, url, filePath, currentLength, totalLength,
                message == null ? "下载失败" : message);
    }

    //下载文件
    protected void onDownloadFile(Response<ResponseBody> response) {
        ResponseBody body = response.body();
        //已停止
        if (isStop) {
            closeBody(body);
            closeBody(response.errorBody());
            onStopListener();
            return;
        }
        BaseManager.Progress listener = baseManager.getProgress(false);
        Handler h = baseManager.getHandleCall();
        //保存地址异常
        if (TextUtils.isEmpty(filePath)) {
            closeBody(body);
            closeBody(response.errorBody());
            downloadType = 2;
            onBack(h, listener, 4, url, filePath, 0, 0, "下载文件路径不能为空");
            return;
        }
        File file = new File(filePath + TEMP_FILE_SUFFIX);
        //请求的起点超过文件长度，可能临时文件已经完整，也可能本地进度无效。
        if (resumeEnabled && response.code() == 416) {
            closeBody(body);
            closeBody(response.errorBody());
            handleRangeNotSatisfiable(response, file, listener, h);
            return;
        }
        //链接失败
        if (!response.isSuccessful() || body == null) {
            closeBody(body);
            closeBody(response.errorBody());
            downloadType = 2;
            if (isStop) {
                onStopListener();
            } else {
                onBack(h, listener, 4, url, filePath, 0, 0,
                        "下载失败，HTTP " + response.code());
            }
            return;
        }

        downloadType = 1;
        long localLength = file.exists() ? file.length() : 0L;
        try {
            //文件起始位置
            long rangeStart = getContentRangeStart(response);
            if (response.code() == 206 && rangeStart != localLength) {
                clearResumeMetadata();
                throw new IOException("服务器返回的续传起点不匹配: "
                        + rangeStart + "/" + localLength);
            }
            //获取客户端本次请求携带的 If-Range。它代表客户端上次记录的文件版本，通常是 ETag 或 Last-Modified。
            String requestValidator = response.raw().request().header("If-Range");
            //获取服务器本次响应中的文件版本标识，优先取强 ETag，没有时取 Last-Modified。
            String responseValidator = getResponseValidator(response);
            //返回 206：文件没变，可以继续追加。
            if (response.code() == 206 && requestValidator != null
                    && responseValidator != null
                    //请求 If-Range： "version-1"
                    //响应 ETag：      "version-2"
                    && !requestValidator.equals(responseValidator)) {
                clearResumeMetadata();
                throw new IOException("远端文件校验标识已变化，不能继续追加");
            }

            // 只有服务器明确返回 206 才能追加；返回 200 表示忽略 Range，必须重新覆盖。
            boolean append = resumeEnabled && localLength > 0 && response.code() == 206;
            currentLength = append ? localLength : 0L;
            totalLength = getTotalLength(response, body.contentLength(), currentLength);
            onBack(h, listener, 1, url, filePath, currentLength, totalLength, "start");

            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("无法创建下载目录: " + parent.getAbsolutePath());
            }
            if (!append) {
                clearResumeMetadata();
            }
            // try-with-resources 确保取消或异常时关闭响应体及文件流。
            try (ResponseBody closeBody = body;
                 InputStream is = closeBody.byteStream();
                 OutputStream os = new FileOutputStream(file, append)) {
                // 完整重下时，FileOutputStream 已先清空旧临时文件；此后才能保存新版本信息。
                // 否则应用在两步之间退出，下次可能把旧临时数据误认为新文件的数据继续追加。
                if (resumeEnabled && responseValidator != null) {
                    saveResumeMetadata(responseValidator);
                }
                int len;
                byte[] buff = new byte[8192];
                while ((len = is.read(buff)) != -1) {
                    if (isStop) {
                        break;
                    }
                    os.write(buff, 0, len);
                    currentLength += len;
                    // 限制进度刷新频率，防止大文件下载产生大量主线程消息。
                    long now = System.currentTimeMillis();
                    if (now - lastProgressTime >= 100
                            || (totalLength >= 0 && currentLength >= totalLength)) {
                        lastProgressTime = now;
                        onBack(h, listener, 2, url, filePath,
                                currentLength, totalLength, "pro");
                    }
                }
                if (!isStop) {
                    os.flush();
                }
            }
            if (isStop) {
                onStopListener();
                return;
            }
            if (totalLength >= 0 && currentLength != totalLength) {
                throw new IOException("下载文件长度不完整: " + currentLength + "/" + totalLength);
            }
            if (isStop) {
                onStopListener();
                return;
            }
            replaceFile(file, new File(filePath));
            clearResumeMetadata();
            downloadType = 2;
            onBack(h, listener, 3, url, filePath,
                    currentLength, totalLength, "completed");
        } catch (Exception e) {
            e.printStackTrace();
            if (isStop) {
                onStopListener();
            } else {
                onBack(h, listener, 4, url, filePath,
                        currentLength, totalLength, e.getMessage());
            }
            downloadType = 2;
        } finally {
            // 创建目录等前置步骤失败时，响应体也必须关闭并归还底层连接。
            closeBody(body);
        }
    }

    private long getTotalLength(Response<ResponseBody> response,
                                long responseLength, long downloadedLength) {
        long rangeTotal = getContentRangeTotal(response);
        if (rangeTotal >= 0) {
            return rangeTotal;
        }
        return responseLength >= 0 ? downloadedLength + responseLength : -1L;
    }

    //获取文件起始位置
    //Content-Range 是服务器返回的响应头，用来说明：
    //本次返回的是整个文件中的哪一段，以及完整文件有多大。
    //例如：HTTP/1.1 206 Partial Content
    //Content-Range: bytes 1000-4999/5000
    //bytes：单位是字节。
    //1000：本次响应从完整文件的第 1000 个字节开始。
    //4999：本次响应到第 4999 个字节结束。(注意字节下标从 0 开始，所以 0～4999 总共是 5000 字节。)
    //5000：完整文件总长度为 5000 字节。
    //Content-Range: bytes */5000 (*/5000 表示没有返回具体的数据区间，但完整文件长度是 5000 字节)
    private long getContentRangeStart(Response<ResponseBody> response) {
        String contentRange = response.headers().get("Content-Range");
        if (contentRange == null) {
            return -1L;
        }
        int spaceIndex = contentRange.indexOf(' ');
        int dashIndex = contentRange.indexOf('-', spaceIndex + 1);
        if (spaceIndex < 0 || dashIndex <= spaceIndex + 1) {
            return -1L;
        }
        try {
            return Long.parseLong(contentRange.substring(spaceIndex + 1, dashIndex));
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    //获取文件总长度
    private long getContentRangeTotal(Response<ResponseBody> response) {
        String contentRange = response.headers().get("Content-Range");
        if (contentRange == null) {
            return -1L;
        }
        int slashIndex = contentRange.lastIndexOf('/');
        if (slashIndex < 0 || slashIndex >= contentRange.length() - 1) {
            return -1L;
        }
        try {
            return Long.parseLong(contentRange.substring(slashIndex + 1));
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    private void handleRangeNotSatisfiable(Response<ResponseBody> response, File tempFile,
                                           BaseManager.Progress listener, Handler handler) {
        if (isStop) {
            onStopListener();
            return;
        }
        long serverLength = getContentRangeTotal(response);
        if (!tempFile.isFile() || serverLength < 0 || tempFile.length() != serverLength) {
            clearResumeMetadata();
            downloadType = 2;
            onBack(handler, listener, 4, url, filePath,
                    tempFile.isFile() ? tempFile.length() : 0L, serverLength,
                    "续传位置已失效，请重新下载");
            return;
        }
        try {
            if (isStop) {
                onStopListener();
                return;
            }
            replaceFile(tempFile, new File(filePath));
            clearResumeMetadata();
            currentLength = serverLength;
            totalLength = serverLength;
            downloadType = 2;
            onBack(handler, listener, 3, url, filePath,
                    serverLength, serverLength, "completed");
        } catch (IOException e) {
            downloadType = 2;
            onBack(handler, listener, 4, url, filePath,
                    tempFile.length(), serverLength, e.getMessage());
        }
    }

    //获取服务器本次响应中的文件版本标识，优先取强 ETag，没有时取 Last-Modified。
    private String getResponseValidator(Response<ResponseBody> response) {
        String etag = response.headers().get("ETag");
        // If-Range 不能使用弱 ETag，弱 ETag 存在时退回 Last-Modified。
        if (!TextUtils.isEmpty(etag) && !etag.startsWith("W/")) {
            return etag;
        }
        String lastModified = response.headers().get("Last-Modified");
        return TextUtils.isEmpty(lastModified) ? null : lastModified;
    }

    //URL 摘要：确认临时文件属于当前下载地址，防止相同保存路径换了 URL 后错误续传。
    //validator：保存强 ETag 或 Last-Modified，下次通过 If-Range 发给服务器，确认远端文件有没有变化
    private void saveResumeMetadata(String validator) {
        File metadataFile = new File(filePath + RESUME_METADATA_SUFFIX);
        String content = createUrlKey(url) + "\n" + validator + "\n";
        try (FileOutputStream output = new FileOutputStream(metadataFile, false)) {
            output.write(content.getBytes(StandardCharsets.UTF_8));
            output.flush();
        } catch (IOException e) {
            // 元数据保存失败不影响当前下载，只会让下次改为完整下载。
            e.printStackTrace();
        }
    }

    //不是清空下载文件，而是清空断点续传的元数据文件：
    private void clearResumeMetadata() {
        File metadataFile = new File(filePath + RESUME_METADATA_SUFFIX);
        if (!metadataFile.exists()) {
            return;
        }
        try (FileOutputStream ignored = new FileOutputStream(metadataFile, false)) {
            // 清空而不是删除，失败时不会影响下载文件本身。
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String createUrlKey(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(String.format("%02x", value & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前系统不支持 SHA-256", e);
        }
    }

    private void replaceFile(File tempFile, File targetFile) throws IOException {
        try {
            // 临时文件与目标文件在同一目录，使用系统 rename 原子替换已有目标。
            Os.rename(tempFile.getAbsolutePath(), targetFile.getAbsolutePath());
        } catch (ErrnoException e) {
            throw new IOException("无法保存下载文件: " + targetFile.getAbsolutePath(), e);
        }
    }

    private void closeBody(ResponseBody body) {
        if (body != null) {
            body.close();
        }
    }

    private void onBack(Handler h, BaseManager.Progress listener, int what, String url, String filePath,
                        long progress, long total, String msg) {
        if (listener != null) {
            try {
                listener.onProgress(what, url, filePath, progress, total, msg);
            } catch (RuntimeException e) {
                // 业务进度回调异常不能打断网络读取或文件收尾。
                e.printStackTrace();
            }
        }

    }

}
