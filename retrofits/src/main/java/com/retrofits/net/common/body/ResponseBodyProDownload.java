package com.retrofits.net.common.body;

import com.retrofits.net.common.ProgressListener;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * 下载响应体进度包装。
 * 外部调用方式不变，通过读取 ResponseBody 自动回调下载进度。
 * 服务器
 * ↓
 * 原始 responseBody.source()
 * ↓
 * Forwarding.read()       统计每次读取的字节数
 * ↓
 * Okio.buffer()           提供缓冲读取能力
 * ↓
 * Retrofit或外部下载代码
 * 下载：数据读进来，所以包装 Source
 * Created by 郭敏 on 2018/3/7 0007.
 */
public class ResponseBodyProDownload extends ResponseBody {
    private static final long PROGRESS_INTERVAL_MILLIS = 100L;

    private final ResponseBody responseBody;
    private final ProgressListener listener;
    private final String url;
    // 下载文件保存路径。
    private final String filePath;
    // ResponseBody 本身只允许消费一次，构造时创建唯一的进度 Source 即可。
    private final BufferedSource bufferedSource;

    public ResponseBodyProDownload(ResponseBody responseBody, ProgressListener listener) {
        this(responseBody, listener, "", "");
    }

    public ResponseBodyProDownload(ResponseBody responseBody, ProgressListener listener,
                                   String filePath) {
        this(responseBody, listener, "", filePath);
    }

    public ResponseBodyProDownload(ResponseBody responseBody, ProgressListener listener,
                                   String url, String filePath) {
        if (responseBody == null) {
            throw new NullPointerException("responseBody == null");
        }
        this.responseBody = responseBody;
        this.listener = listener;
        this.url = url == null ? "" : url;
        this.filePath = filePath == null ? "" : filePath;
        //是服务器返回的原始响应数据流。OkHttp 从这个 Source 中读取下载内容。
        BufferedSource source = responseBody.source();
        //在原始下载数据流外面加一层“进度统计包装”。
        Source progressSource = new Forwarding(source);
        //给进度数据流再套一层缓冲，转换成 BufferedSource。
        //因为 ResponseBody.source() 方法要求返回的是：BufferedSource
        //而 ForwardingSource 本身只是普通的：Source 所以需要使用：Okio.buffer(progressSource)
        this.bufferedSource = Okio.buffer(progressSource);
    }

    @Override
    public MediaType contentType() {
        return responseBody.contentType();
    }

    @Override
    public long contentLength() {
        return responseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
        return bufferedSource;
    }

    // 统计实际从网络响应体读取的字节数。
    private class Forwarding extends ForwardingSource {
        private final long totalLength = responseBody.contentLength();
        private long totalBytesRead;
        private long lastNotifyTime;
        private boolean completionNotified;

        Forwarding(Source delegate) {
            super(delegate);
        }

        /**
         *
         * @param sink      本次读取的数据要放入的缓冲区。
         * @param byteCount 调用方本次最多希望读取多少字节。
         * @return
         * @throws IOException
         */
        @Override
        public long read(Buffer sink, long byteCount) throws IOException {
            //返回值：本次实际读取的字节数。
            //返回 -1：数据已经全部读取完，也就是到达 EOF(EOF 是 End Of File，中文就是“文件或数据流结束”。)
            long bytesRead = super.read(sink, byteCount);
            if (bytesRead == -1) {
                // 某些调用方可能在 EOF 后再次读取，完成状态只能发送一次。
                if (!completionNotified) {
                    completionNotified = true;
                    notifyProgress(3, totalBytesRead, totalLength, "completed");
                }
                return -1;
            }

            totalBytesRead += bytesRead;
            long now = System.currentTimeMillis();
            // 限制刷新频率；读到已知总长度时立即发送最后一次进度。
            if (now - lastNotifyTime >= PROGRESS_INTERVAL_MILLIS
                    || (totalLength >= 0 && totalBytesRead >= totalLength)) {
                lastNotifyTime = now;
                notifyProgress(2, totalBytesRead, totalLength, "pro");
            }
            return bytesRead;
        }
    }

    // 业务进度监听异常不能中断响应体读取。
    private void notifyProgress(int what, long progress, long total, String message) {
        if (listener == null) {
            return;
        }
        try {
            listener.onProgress(what, url, filePath, progress, total, message);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}
