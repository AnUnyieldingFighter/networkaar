package com.retrofits.net.common.body;


import com.retrofits.net.common.ProgressListener;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSink;
import okio.ForwardingSource;
import okio.Okio;
import okio.Sink;
import okio.Source;
import okio.Timeout;

/**
 * 下载 需要进度指示时使用
 * Created by 郭敏 on 2018/3/7 0007.
 */

public class ResponseBodyProDownload extends ResponseBody {
    private final ResponseBody responseBody;
    private final ProgressListener listener;
    //上传文件path
    private String upFilePath;
    private BufferedSource bufferedSource;

    public ResponseBodyProDownload(ResponseBody responseBody, ProgressListener listener) {
        this.responseBody = responseBody;
        this.listener = listener;
        this.upFilePath = "";
    }

    public ResponseBodyProDownload(ResponseBody responseBody, ProgressListener listener, String upFilePath) {
        this.responseBody = responseBody;
        this.listener = listener;
        this.upFilePath = upFilePath;
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

            // 空安全，防止 null 崩溃
            if (listener != null) {
                listener.onProgress(what, "", upFilePath, totalBytesRead, length,"");
            }
            return bytesRead;
        }

    }

}
