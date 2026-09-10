package com.retrofits.net.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retrofits.net.common.body.RequestBodyProUpload;
import com.retrofits.net.common.body.ResponseBodyProDownload;
import com.retrofits.net.common.custom.JacksonFactory;
import com.retrofits.utiles.RLog;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.EventListener;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Created by Administrator on 2016/9/7.
 * OkHttpClient：必须尽量复用:
 * DNS 查询 → TCP 连接 → TLS 握手 → HTTP 请求 ,复用 OkHttpClient 后，后续请求可以使用连接池中的已有连接
 * Retrofit：建议复用
 * LoginApi：建议复用，但不复用影响很小
 * Call：不能复用，每次请求都要新建
 * <p>
 * 完整网络请求用时日志：
 * RLog.DBUG = true;
 * 从请求开始计时，到响应体读取完成、关闭或者请求失败时打印，普通请求、上传和下载都支持。
 */
public class BaseNetSource {
    //现在普通接口缓存 Retrofit，所以connectionPool与dispatcher这两个共享对象主要继续服务于不同配置的普通客户端以及上传、
    //下载客户端。它们不是必须项，但保留后能统一管理底层连接和线程，开销也很小。
    // 连接池:每个任务可以使用不同的证书和进度配置，但底层连接与调度线程应统一复用。
    private static final ConnectionPool connectionPool = new ConnectionPool();
    //- 管理异步请求线程。
    //- 控制同时执行的请求数量。
    //- 控制同一主机的并发数量。
    //- 对超过限制的请求进行排队。
    //例如一次向同一服务器发起十几个异步请求，共享 Dispatcher 后，会由同一个调度器统一控制并发，
    // 而不是每个 OkHttpClient 各自创建线程和计算并发数量。
    private static final Dispatcher dispatcher = new Dispatcher();
    // 普通请求按实际网络配置复用 Retrofit，避免并发请求时重复初始化。
    //可以安全复用的情况：
    //baseUrl 相同。
    //Jackson 配置相同。
    //SSL 和证书配置相同。
    //OkHttp 拦截器配置相同。
    //不包含某个请求专属的监听器或文件路径。
    private static final Map<List<Object>, Retrofit> retrofitCache = new HashMap<>();

    protected ProgressListener listener;
    protected OkHttpClient okHttpClient;
    //上传文件path 或者  下载文件 保存的地址
    protected String upFilePath;
    //1 上传  2 下载 0 普通请求
    protected int requestType;

    public void setProgressListener(int upType, ProgressListener listener, String upFilePath) {
        this.requestType = upType;
        this.listener = listener;
        this.upFilePath = upFilePath;
    }

    public void setProgressType(int upType) {
        this.requestType = upType;
    }

    public Retrofit getRetrofit(BaseUrl constraint) {
        if (constraint == null) {
            throw new IllegalArgumentException("BaseUrl 不能为空");
        }
        String baseUrl = constraint.getUrl();
        if (baseUrl == null || baseUrl.length() == 0) {
            throw new IllegalArgumentException("BaseUrl 地址不能为空");
        }
        final int requestType = this.requestType;
        if (requestType < 0 || requestType > 2) {
            throw new IllegalArgumentException("不支持的网络请求类型: " + requestType);
        }
        if (requestType == 0) {
            List<Object> cacheKey = createCacheKey(constraint, baseUrl);
            synchronized (retrofitCache) {
                Retrofit retrofit = retrofitCache.get(cacheKey);
                if (retrofit == null) {
                    retrofit = createRetrofit(constraint, baseUrl);
                    retrofitCache.put(cacheKey, retrofit);
                }
                return retrofit;
            }
        }
        // 上传、下载包含单次任务的进度监听器，因此不缓存完整 Retrofit。
        return createRetrofit(constraint, baseUrl);
    }

    private Retrofit createRetrofit(BaseUrl constraint, String baseUrl) {
        return new Retrofit.Builder()
                // Retrofit 要求 baseUrl 以 '/' 结尾，提前校验可避免运行时构建失败。
                .baseUrl(baseUrl)
                .addConverterFactory(getJsonMapper())
                .client(getOkHttpClient(constraint))
                .build();
    }

    private List<Object> createCacheKey(BaseUrl constraint, String baseUrl) {
        // 使用不可变列表作为缓存键，避免不同 URL 或证书配置错误共用客户端。
        return Collections.unmodifiableList(Arrays.<Object>asList(
                getClass(),
                baseUrl,
                constraint.isSSL(),
                constraint.isTrustAllCertificates(),
                immutableList(constraint.getSSLCertificates()),
                immutableList(constraint.getHostName()),
                RLog.DBUG));
    }

    private List<String> immutableList(String[] values) {
        if (values == null || values.length == 0) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(Arrays.asList(values.clone()));
    }

    protected Converter.Factory getJsonMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        //属性为NULL不序列化
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //忽略多余字段
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //JacksonConverterFactory factory = JacksonConverterFactory.create(objectMapper);
        JacksonFactory factory = JacksonFactory.create(objectMapper);
        return factory;
    }

    //设置OkHttpClient
    protected OkHttpClient getOkHttpClient(BaseUrl constraint) {
        // 客户端保留本次任务的拦截器和 SSL 配置，同时共享连接池与调度线程。
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .dispatcher(dispatcher);

        if (constraint.isReqTimeContinue()) {
            // 每个 Call 使用独立的监听器，完整统计排队、连接、发送和接收响应体的总用时。
            builder.eventListenerFactory(call -> new NetworkTimeListener());
            //
            //调试日志只记录请求元数据，避免读取整个请求/响应体。
            //builder.addInterceptor(new Network());
        }
        //添加请求头
        setReqHead(builder);
        setResInterceptor(builder);
        builder = setSSl(constraint, builder);
        setTimeOut(builder);
        okHttpClient = builder.build();
        return okHttpClient;
    }

    //设置超时
    protected void setTimeOut(OkHttpClient.Builder builder) {
        if (requestType == 0) {
            //普通请求
            builder.connectTimeout(60, TimeUnit.SECONDS);
            builder.readTimeout(60, TimeUnit.SECONDS);
            builder.writeTimeout(60, TimeUnit.SECONDS);
        } else {
            builder.connectTimeout(2, TimeUnit.MINUTES);   // 连接超时
            builder.readTimeout(10, TimeUnit.MINUTES);    // 读超时
            builder.writeTimeout(10, TimeUnit.MINUTES);    // 写超时（上传核心）
            //builder.retryOnConnectionFailure(true)         // 开启失败自动重试
        }

    }

    //设置证书
    protected OkHttpClient.Builder setSSl(BaseUrl constraint, OkHttpClient.Builder builder) {
        boolean isSSl = constraint.isSSL();
        String url = constraint.getUrl();
        if (isSSl && url != null && url.regionMatches(true, 0, "https://", 0, 8)) {
            builder = new SSL().setSSL(builder, constraint.getContext(),
                    constraint.getSSLCertificates(), constraint.getHostName(),
                    constraint.isTrustAllCertificates());
        }
        return builder;
    }

    //设置请求head
    protected void setReqHead(OkHttpClient.Builder builder) {
        if (requestType == 0) {
            builder.addInterceptor(new RequestHeader());
        } else {
            builder.addInterceptor(new RequestHeaderUpload());
        }

    }

    //拦截 数据返回
    protected void setResInterceptor(OkHttpClient.Builder builder) {
        //
        switch (requestType) {
            case 0:
                break;
            case 1:
                //上传
                if (listener != null) {
                    // 上传进度必须包装请求体，而不是响应体。
                    builder.addInterceptor(new ProgressUpload(listener, upFilePath));
                }
                break;
            case 2:
                //下载
                if (listener != null) {
                    builder.addInterceptor(new ProgressDownload(listener, upFilePath));
                }
                break;

        }

    }

    //普通请求添加头部
    public class RequestHeader implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request()
                    .newBuilder()
                    .addHeader("Accept", "*/*")
                    // .addHeader("Cookie", "add cookies here")
                    .build();
            Response response = chain.proceed(request);
            return response;
        }
    }

    //上传用
    public class RequestHeaderUpload implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request()
                    .newBuilder()
                    //绝对不要手动加 Content-Type！！
                    //上传文件时，系统会自动生成：multipart/form-data
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Accept", "*/*")
                    .build();
            return chain.proceed(request);
        }
    }

    // 上传进度拦截：包装请求体并统计实际写入网络的字节数。
    public class ProgressUpload implements Interceptor {
        private final ProgressListener progressListener;
        private final String filePath;

        public ProgressUpload() {
            this(listener, upFilePath);
        }

        public ProgressUpload(ProgressListener progressListener, String filePath) {
            this.progressListener = progressListener;
            this.filePath = filePath;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            RequestBody requestBody = request.body();
            if (requestBody == null || requestBody instanceof RequestBodyProUpload) {
                return chain.proceed(request);
            }
            // 上传进度要统计请求体写入网络的字节数，不能包装服务器返回的响应体。
            Request progressRequest = request.newBuilder()
                    .method(request.method(), new RequestBodyProUpload(
                            requestBody, progressListener, filePath))
                    .build();
            return chain.proceed(progressRequest);
        }
    }

    //下载进度拦截
    public class ProgressDownload implements Interceptor {
        private final ProgressListener progressListener;
        private final String filePath;

        public ProgressDownload() {
            this(listener, upFilePath);
        }

        public ProgressDownload(ProgressListener progressListener, String filePath) {
            this.progressListener = progressListener;
            this.filePath = filePath;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            okhttp3.Response orginalResponse = chain.proceed(chain.request());
            if (!orginalResponse.isSuccessful()) {
                // HTTP 错误响应交给业务层处理，不能误报为下载已经开始。
                return orginalResponse;
            }
            ResponseBody responseBody = orginalResponse.body();
            if (responseBody == null) {
                return orginalResponse;
            }
            String requestUrl = orginalResponse.request().url().toString();
            // 响应体可读取后立即通知下载开始，后续由包装体发送进行中和完成事件。
            if (progressListener != null) {
                try {
                    progressListener.onProgress(1, requestUrl, filePath, 0,
                            responseBody.contentLength(), "start");
                } catch (RuntimeException e) {
                    // 业务进度回调异常不能中断实际下载请求。
                    e.printStackTrace();
                }
            }
            return orginalResponse.newBuilder()
                    .body(new ResponseBodyProDownload(
                            responseBody, progressListener, requestUrl, filePath))
                    .build();
        }
    }

    //网络请求拦截（日志打印）
    public static class Network implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            RequestBody requestBody = request.body();
            long startTime = System.currentTimeMillis();
            Response response = chain.proceed(request);
            ResponseBody responseBody = response.body();
            long time = System.currentTimeMillis() - startTime;
            RLog.e("--", "\n响应: code:" + response.code()
                    + "\nurl：" + response.request().url()
                    + "\n请求头: " + redactHeaders(request.headers())
                    + "\n请求体长度: " + (requestBody == null ? 0 : requestBody.contentLength())
                    + "\n请求体类型: " + (requestBody == null ? null : requestBody.contentType())
                    + "\n响应体长度: " + (responseBody == null ? 0 : responseBody.contentLength())
                    + "\n响应体类型: " + (responseBody == null ? null : responseBody.contentType())
                    + "\n响应时间：" + time + "毫秒");
            return response;
        }

        // 调试日志中隐藏认证信息和 Cookie，避免敏感数据落盘。
        private String redactHeaders(okhttp3.Headers headers) {
            StringBuilder result = new StringBuilder();
            for (String name : headers.names()) {
                String value = name.equalsIgnoreCase("Authorization")
                        || name.equalsIgnoreCase("Cookie")
                        || name.equalsIgnoreCase("Set-Cookie")
                        ? "<redacted>"
                        : headers.get(name);
                result.append(name).append(": ").append(value).append("; ");
            }
            return result.toString();
        }
    }

    // 统计一次 OkHttp Call 的完整生命周期，不读取或缓存请求体、响应体。
    //统计请求生命周期、耗时、DNS、连接、字节数
    public class NetworkTimeListener extends EventListener {
        private long startTimeNanos;

        @Override
        public void callStart(Call call) {
            startTimeNanos = System.nanoTime();
        }

        @Override
        public void callEnd(Call call) {
            printTime(call, startTimeNanos, "完成", null);
        }

        @Override
        public void callFailed(Call call, IOException ioe) {
            printTime(call, startTimeNanos, "失败", ioe);
        }


    }

    //打印请求时间
    protected void printTime(Call call, long startTimeNanos, String result, IOException ioe) {
        long timeMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        String error = ioe == null ? "" : "\n失败原因：" + ioe;
        RLog.e("网络请求完整用时", "\nurl：" + call.request().url()
                + "\n完整请求用时：" + timeMillis + "毫秒"
                + "\n请求结果：" + result
                + error);
    }

}
