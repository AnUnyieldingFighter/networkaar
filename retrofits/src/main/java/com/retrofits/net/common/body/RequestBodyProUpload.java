package com.retrofits.net.common.body;


import com.retrofits.net.common.ProgressListener;
import com.retrofits.utiles.RLog;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * 上传 需要进度指示时使用
 * Created by 郭敏 on 2018/3/7 0007.
 */

public class RequestBodyProUpload extends RequestBody {
    private final ProgressListener listener;
    //上传文件path
    private String upFilePath;
    // 原始的 RequestBody（文件、字符串、流 都可以）
    private RequestBody requestBody;

    public RequestBodyProUpload(RequestBody requestBody, ProgressListener listener) {
        this.requestBody = requestBody;
        this.listener = listener;
        this.upFilePath = "";
    }

    public RequestBodyProUpload(RequestBody requestBody, ProgressListener listener, String upFilePath) {
        this.requestBody = requestBody;
        this.listener = listener;
        this.upFilePath = upFilePath;
    }


    @Override
    public MediaType contentType() {
        return requestBody.contentType();
    }

    @Override
    public long contentLength() {
        try {
            return requestBody.contentLength();
        } catch (IOException e) {
            RLog.e("上传文件", e.getMessage());
            return -1;
        }
    }

    //  标记是否已经回调过“开始上传”
    private boolean hasStart = false;

    //1：开始 2：进行中 3：完成 4：出错 5:停止下载
    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        if (!hasStart) {
            hasStart = true;
            listener.onProgress(1, "", upFilePath, 0, contentLength(),"start");
        }
        // 2. 包装 Sink（关键！）
        Sink progressSink = new ForwardingUpload(sink);
        // 3. 包装成 BufferedSink 写入真实数据
        BufferedSink bufferedSink = Okio.buffer(progressSink);
        requestBody.writeTo(bufferedSink);
        bufferedSink.flush(); // 必须刷新
    }


    //上传
    class ForwardingUpload extends ForwardingSink {
        private long bytesUploaded = 0; // 已上传
        private long totalLength = contentLength();       // 总长度

        public ForwardingUpload(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);//上传
            bytesUploaded += byteCount;     // 统计
            if (bytesUploaded == totalLength) {
                //上传完成
                listener.onProgress(3, "", upFilePath, bytesUploaded, totalLength,"completed");// 回调进度
            } else
                //上传中
                listener.onProgress(2, "", upFilePath, bytesUploaded, totalLength,"pro");// 回调进度
        }
    }


}
