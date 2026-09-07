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
 * 下载 需要进度指示时使用
 * Created by 郭敏 on 2018/3/7 0007.
 */

public class ResponseBodyProDownload extends ResponseBody {
    private final ResponseBody responseBody;
    private final ProgressListener listener;
    private final String url;
    // 下载文件保存路径
    private final String filePath;
    // source 可能被不同线程访问，使用 volatile 保证安全发布。
    private volatile BufferedSource bufferedSource;

    public ResponseBodyProDownload(ResponseBody responseBody, ProgressListener listener) {
        if (responseBody == null) {
            throw new NullPointerException("responseBody == null");
        }
        this.responseBody = responseBody;
        this.listener = listener;
        this.url = "";
        this.filePath = "";
    }

    public ResponseBodyProDownload(ResponseBody responseBody, ProgressListener listener, String upFilePath) {
        this(responseBody, listener, "", upFilePath);
    }

    public ResponseBodyProDownload(ResponseBody responseBody, ProgressListener listener,
                                   String url, String filePath) {
        if (responseBody == null) {
            throw new NullPointerException("responseBody == null");
        }
        this.responseBody = responseBody;
        this.listener = listener;
        this.url = url;
        this.filePath = filePath;
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
        if (bufferedSource == null) {
            synchronized (this) {
                if (bufferedSource == null) {
                    Source source = new Forwarding(responseBody.source());
                    bufferedSource = Okio.buffer(source);
                }
            }
        }
        return bufferedSource;
    }

    //下载
    class Forwarding extends ForwardingSource {
        //已下载（已上传）字节
        private long totalBytesRead = 0L;
        private long lastNotifyTime;

        public Forwarding(Source delegate) {
            super(delegate);
        }

        //默认 byteCount=8192
        @Override
        public long read(Buffer sink, long byteCount) throws IOException {
            long bytesRead = super.read(sink, byteCount);
            boolean isDone = (bytesRead == -1);
            if (!isDone) {
                totalBytesRead += bytesRead;
            }
            long length = responseBody.contentLength();
            int what = isDone ? 3 : 2;//下载完成/下载中

            // 大文件每次读取都会进入这里，限制刷新频率以免主线程消息队列堆积。
            long now = System.currentTimeMillis();
            boolean shouldNotify = isDone
                    || now - lastNotifyTime >= 100
                    || (length >= 0 && totalBytesRead >= length);
            if (listener != null && shouldNotify) {
                lastNotifyTime = now;
                try {
                    listener.onProgress(what, url, filePath, totalBytesRead, length, "");
                } catch (RuntimeException e) {
                    // 业务层进度处理失败不能中断响应体读取。
                    e.printStackTrace();
                }
            }
            return bytesRead;
        }

    }

}
