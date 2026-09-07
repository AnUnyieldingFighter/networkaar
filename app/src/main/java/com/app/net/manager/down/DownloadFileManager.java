package com.app.net.manager.down;

import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;

import com.app.net.common.RequestResultThreadDownloadListener;
import com.app.net.common.UrlManger;
import com.retrofits.net.common.BaseNetSource;
import com.retrofits.net.common.ProgressListener;
import com.retrofits.net.common.RequestBack;
import com.retrofits.net.common.thread.NetSourceThreadPool;
import com.retrofits.net.manager.BaseManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * 下载
 *
 * 使用方法：
 * request(fileUrl, filePath, true);  // 开启断点续传
 * request(fileUrl, filePath, false); // 普通下载，每次都从头下载
 * request(fileUrl, filePath);        // 保持原有行为，默认开启断点续传
 *
 * Created by Administrator on 2016/9/7.
 */
public class DownloadFileManager extends BaseManager {

    public DownloadFileManager(RequestBack requestBack) {
        super(requestBack);
    }



    private RequestResultThreadDownloadListener listener;
    // request2 没有使用线程监听器，需要单独保存 Call 才能取消。
    private Call<ResponseBody> directDownloadCall;

    public void onStop() {
        if (listener != null) {
            listener.onStopDownload();
        }
        if (directDownloadCall != null && !directDownloadCall.isCanceled()) {
            // request2 使用 Retrofit 异步请求，需要直接取消它持有的 Call。
            directDownloadCall.cancel();
        }
    }

    private String fileUrlTamp = "http://img-smarthos.hztywl.cn/EDU_COURSE_201908_mPn7ZJR_uqZKQ.pdf";
    private String fialePathTamp = "/storage/emulated/0/Download/test.pdf";
    public void request(String fileUrl,String fialePath) {
        // 保持旧调用方式的行为不变，默认开启断点续传。
        request(fileUrl, fialePath, true);
    }

    public void request(String fileUrl, String fialePath, boolean enableResume) {

        BaseNetSource source = new BaseNetSource();
        Retrofit retrofit = source.getRetrofit(new UrlManger());
        DownloadApi service = retrofit.create(DownloadApi.class);
        // 临时文件存在时从已有长度继续下载，避免把完整响应重复追加到旧文件。
        File tempFile = new File(fialePath + RequestResultThreadDownloadListener.TEMP_FILE_SUFFIX);
        long downloadedLength = enableResume && tempFile.exists() ? tempFile.length() : 0L;
        File metadataFile = new File(
                fialePath + RequestResultThreadDownloadListener.RESUME_METADATA_SUFFIX);
        String validator = enableResume ? readResumeValidator(metadataFile, fileUrl) : null;
        // 没有匹配的 ETag/Last-Modified 时不续传，防止拼接不同版本的远端文件。
        boolean canResume = enableResume && downloadedLength > 0 && validator != null;
        String range = canResume ? "bytes=" + downloadedLength + "-" : null;
        Call<ResponseBody> call = service.download(range, canResume ? validator : null, fileUrl);
        listener = new RequestResultThreadDownloadListener(this, call);
        listener.setDownloadFile(fileUrl, fialePath);
        listener.setResumeEnabled(enableResume);
        listener.start();
    }

    public void request2(String fileUrl,String fialePath) {

        BaseNetSource source = new BaseNetSource();
        ProgressListener progressListener = getProgress(false);
        // 网络响应读完不代表文件已经落盘，完成事件由 saveFile 成功后统一发送。
        ProgressListener networkProgress = (what, url, filePath, progress, total, msg) -> {
            if (what != 3) {
                progressListener.onProgress(what, fileUrl, filePath, progress, total, msg);
            }
        };
        source.setProgressListener(2, networkProgress, fialePath);
        // 必须先设置进度配置，再构建 Retrofit 客户端，拦截器才能生效。
        Retrofit retrofit = source.getRetrofit(new UrlManger());
        DownloadApi service = retrofit.create(DownloadApi.class);
        Call<ResponseBody> call = service.download2(fileUrl);
        directDownloadCall = call;
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                ResponseBody body = response.body();
                if (!response.isSuccessful() || body == null) {
                    closeBody(body);
                    closeBody(response.errorBody());
                    int state = call.isCanceled() ? 5 : 4;
                    progressListener.onProgress(state, fileUrl, fialePath,
                            0, 0, state == 5 ? "stop" : "下载失败，HTTP " + response.code());
                    directDownloadCall = null;
                    return;
                }
                // 文件读写统一放入已有后台线程池，避免为每次下载单独创建线程。
                NetSourceThreadPool.getInstance().execute(
                        () -> saveFile(body, fileUrl, fialePath, progressListener, call));
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                int state = call.isCanceled() ? 5 : 4;
                String message = t == null ? "下载失败" : t.getMessage();
                progressListener.onProgress(state, fileUrl, fialePath,
                        0, 0, state == 5 ? "stop"
                                : (message == null ? "下载失败" : message));
                directDownloadCall = null;
            }
        });
    }

    // 保存文件
    private void saveFile(ResponseBody body, String url, String path,
                          ProgressListener progressListener, Call<ResponseBody> call) {
        File targetFile = new File(path);
        File tempFile = new File(path + ".download");
        long totalLength = body.contentLength();
        try {
            File parent = targetFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("无法创建下载目录: " + parent.getAbsolutePath());
            }
            // 先写临时文件，失败时不会留下看似完整的目标文件。
            try (ResponseBody closeBody = body;
                 InputStream is = closeBody.byteStream();
                 FileOutputStream fos = new FileOutputStream(tempFile, false)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    if (call.isCanceled()) {
                        break;
                    }
                    fos.write(buffer, 0, len);
                }
                if (!call.isCanceled()) {
                    fos.flush();
                }
            }
            if (call.isCanceled()) {
                progressListener.onProgress(5, url, path,
                        tempFile.length(), totalLength, "stop");
                return;
            }
            if (totalLength >= 0 && tempFile.length() != totalLength) {
                throw new IllegalStateException(
                        "下载文件长度不完整: " + tempFile.length() + "/" + totalLength);
            }
            replaceFile(tempFile, targetFile);
            long fileLength = targetFile.length();
            progressListener.onProgress(3, url, path, fileLength,
                    totalLength >= 0 ? totalLength : fileLength, "completed");
        } catch (Exception e) {
            e.printStackTrace();
            String message = e.getMessage();
            int state = call.isCanceled() ? 5 : 4;
            progressListener.onProgress(state, url, path, 0, totalLength,
                    state == 5 ? "stop"
                            : (message == null ? "保存下载文件失败" : message));
        } finally {
            // 创建目录等前置步骤失败时也必须关闭响应体。
            closeBody(body);
            directDownloadCall = null;
        }
    }

    private void replaceFile(File tempFile, File targetFile) throws IOException {
        try {
            // 同目录原子替换：失败时旧目标文件和临时文件都不会被提前删除。
            Os.rename(tempFile.getAbsolutePath(), targetFile.getAbsolutePath());
        } catch (ErrnoException e) {
            throw new IOException("无法保存下载文件: " + targetFile.getAbsolutePath(), e);
        }
    }



    private String readResumeValidator(File metadataFile, String expectedUrl) {
        if (!metadataFile.isFile()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(metadataFile), StandardCharsets.UTF_8))) {
            String savedUrl = reader.readLine();
            String validator = reader.readLine();
            String expectedUrlKey = RequestResultThreadDownloadListener.createUrlKey(expectedUrl);
            if (!expectedUrlKey.equals(savedUrl) || TextUtils.isEmpty(validator)) {
                return null;
            }
            return validator;
        } catch (IOException e) {
            // 元数据不可读时退回完整下载，不能冒险续传未知版本的文件。
            return null;
        }
    }

    private void closeBody(ResponseBody body) {
        if (body != null) {
            body.close();
        }
    }

}
