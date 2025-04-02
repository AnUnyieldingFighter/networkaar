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
 * 上传 需要进度指示时使用
 * Created by 郭敏 on 2018/3/7 0007.
 */

public class ProgressResponseBody extends ResponseBody {
    private final ResponseBody responseBody;
    private final ProgressListener listener;
    //上传文件path
    private String upFilePath;
    private BufferedSource bufferedSource;

    public ProgressResponseBody(ResponseBody responseBody, ProgressListener listener) {
        this.responseBody = responseBody;
        this.listener = listener;
        this.upFilePath = "";
    }

    public ProgressResponseBody(ResponseBody responseBody, ProgressListener listener, String upFilePath) {
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
        if (null == bufferedSource) {
            Source source = new Forwarding(responseBody.source());
            bufferedSource = Okio.buffer(source);
        }
        return bufferedSource;
    }

    class Forwarding extends ForwardingSource {
        //已下载（已上传）字节
        private long totalBytesRead = 0L;

        public Forwarding(Source delegate) {
            super(delegate);
        }

        //默认 byteCount=8192
        @Override
        public long read(Buffer sink, long byteCount) throws IOException {
            byteCount = 50;
            long bytesRead = super.read(sink, byteCount);
            //true  已上传 完成
            boolean isDone = (bytesRead == -1);
            if (!isDone) {
                totalBytesRead += bytesRead;
            }
            long length = responseBody.contentLength();
            int what = 2;
            if (isDone) {
                what = 3;
            }
            listener.onProgress(what, "", upFilePath,
                    totalBytesRead, length);
            return bytesRead;
        }
    }

}
