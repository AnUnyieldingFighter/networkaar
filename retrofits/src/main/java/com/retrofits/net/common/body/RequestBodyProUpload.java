package com.retrofits.net.common.body;

import com.retrofits.net.common.ProgressListener;
import com.retrofits.utiles.RLog;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * 文件/Multipart/其他 RequestBody
 *         ↓
 * RequestBodyProUpload
 *         ↓
 * BufferedSink
 *         ↓
 * ForwardingUpload 统计字节
 *         ↓
 * OkHttp 下层 Sink
 *         ↓
 * 网络
 * 上传：数据写出去，所以包装 Sink
 * Created by 郭敏 on 2018/3/7 0007.
 */
public class RequestBodyProUpload extends RequestBody {
    //每100毫秒 发送一次
    private final long PROGRESS_INTERVAL_MILLIS = 100L;
    //回调
    private final ProgressListener listener;
    // 上传文件路径，用于区分进度属于哪个文件。
    private final String upFilePath;
    // 原始 RequestBody，可以是文件、字节数组或流。（如果是文件上传 一定要包含文件，否则就失去了意义）
    private final RequestBody requestBody;


    public RequestBodyProUpload(RequestBody requestBody, ProgressListener listener) {
        this(requestBody, listener, "");
    }

    public RequestBodyProUpload(RequestBody requestBody, ProgressListener listener,
                                String upFilePath) {
        if (requestBody == null) {
            throw new NullPointerException("requestBody == null");
        }
        this.requestBody = requestBody;
        this.listener = listener;
        this.upFilePath = upFilePath == null ? "" : upFilePath;
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
            return -1L;
        }
    }

    // 一个请求只发送一次开始状态。
    private final AtomicBoolean hasStart = new AtomicBoolean(false);

    //完成一次请求体上传，并在开始、进行中、写入完成三个阶段回调进度
    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        long totalLength = contentLength();
        if (hasStart.compareAndSet(false, true)) {
            //发送“开始上传”回调：
            notifyProgress(1, 0, totalLength, "start");
        }
        //用 ForwardingUpload 包装 OkHttp 提供的原始 sink。
        //它不会保存文件，而是在数据经过时统计字节数：
        ForwardingUpload progressSink = new ForwardingUpload(sink, totalLength);
        //给 progressSink 增加缓冲能力。
        //要求接收 BufferedSink，因此需要通过 Okio.buffer() 包装。数据会先写入缓冲区，
        //达到一定大小后再交给 ForwardingUpload
        BufferedSink bufferedSink = Okio.buffer(progressSink);
        //这是实际写入上传数据的地方。
        //如果 requestBody 是文件请求体，这一步会读取文件并不断写入 bufferedSink。
        //每次数据继续写入 ForwardingUpload 时，都会累计 bytesUploaded += byteCount;
        requestBody.writeTo(bufferedSink);
        //将缓冲区中剩余的数据全部写入下层 OkHttp sink。
        //如果不调用 flush()，最后不足一个缓冲区的数据可能还没有经过 ForwardingUpload，进度数字也可能不完整
        bufferedSink.flush();
        // 在这里统一发送完成状态，空内容和未知长度的请求体也能正常完成。
        notifyProgress(3, progressSink.getBytesUploaded(), totalLength, "completed");
    }

    @Override
    public boolean isDuplex() {
        return requestBody.isDuplex();
    }

    @Override
    public boolean isOneShot() {
        return requestBody.isOneShot();
    }

    private void notifyProgress(int what, long progress, long total, String message) {
        if (listener == null) {
            return;
        }
        try {
            listener.onProgress(what, "", upFilePath, progress, total, message);
        } catch (RuntimeException e) {
            // 进度监听异常不能中断真实上传。
            RLog.e("上传进度回调异常", e.getMessage());
        }
    }

    // 统计实际写入下层 Sink 的字节数。
    private class ForwardingUpload extends ForwardingSink {
        private final long totalLength;
        private long bytesUploaded;
        private long lastProgressTime;

        ForwardingUpload(Sink delegate, long totalLength) {
            super(delegate);
            this.totalLength = totalLength;
        }

        /**
         *
         * @param source    本次准备写出的数据缓冲区。
         * @param byteCount 本次准备写出的字节数量。
         * @throws IOException
         */
        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            //先执行 super.write() 写入，成功后再增加进度。
            //如果写入失败并抛出 IOException，本次字节就不会被错误地统计为已上传。
            super.write(source, byteCount);
            bytesUploaded += byteCount;

            long now = System.currentTimeMillis();
            // 限制刷新频率；最后一段数据不受时间间隔限制。
            if (now - lastProgressTime >= PROGRESS_INTERVAL_MILLIS
                    || (totalLength >= 0 && bytesUploaded >= totalLength)) {
                lastProgressTime = now;
                notifyProgress(2, bytesUploaded, totalLength, "pro");
            }
        }

        long getBytesUploaded() {
            return bytesUploaded;
        }
    }
}
