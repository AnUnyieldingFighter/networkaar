package com.retrofits.net.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retrofits.net.common.body.ProgressResponseBody;
import com.retrofits.net.common.custom.JacksonFactory;
import com.retrofits.utiles.RLog;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Created by Administrator on 2016/9/7.
 */
public class BaseNetSource {
    protected ProgressListener listener;
    protected OkHttpClient okHttpClient;
    //上传文件path
    protected String upFilePath;

    public void setProgressListener(ProgressListener listener, String upFilePath) {
        this.listener = listener;
        this.upFilePath = upFilePath;
    }

    public Retrofit getRetrofit(BaseUrl constraint) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(constraint.getUrl())
                .addConverterFactory(getJsonMapper())
                .client(getOkHttpClient(constraint))
                .build();
        return retrofit;
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
        if (okHttpClient == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.addInterceptor(new RequestHeader());
            if (RLog.DBUG) {
                builder.addInterceptor(new Network());
            }
            if (listener != null) {
                builder.addInterceptor(new Progress());
            }
            builder = setSSl(constraint, builder);
            setTimeOut(builder);
            okHttpClient = builder.build();
        }
        return okHttpClient;
    }

    //设置超时
    protected void setTimeOut(OkHttpClient.Builder builder) {
        builder.connectTimeout(60, TimeUnit.SECONDS);
        builder.readTimeout(60, TimeUnit.SECONDS);
        builder.writeTimeout(60, TimeUnit.SECONDS);
    }

    //设置证书
    protected OkHttpClient.Builder setSSl(BaseUrl constraint, OkHttpClient.Builder builder) {
        boolean isSSl = constraint.isSSL();
        String url = constraint.getUrl();
        if (isSSl && url.startsWith("https")) {
            builder = new SSL().setSSL(builder, constraint.getContext(),
                    constraint.getSSLCertificates(),constraint.getHostName());
        }
        return builder;
    }

    //网络请求添加头部
    public class RequestHeader implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request()
                    .newBuilder()
                    .addHeader("Content-Type", "application/json; charset=UTF-8")
                    // .addHeader("Accept-Encoding", "gzip, deflate")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Accept", "*/*")
                    // .addHeader("Cookie", "add cookies here")
                    .build();
            Response response = chain.proceed(request);
            return response;
        }
    }

    //上传下载进度拦截
    public class Progress implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            okhttp3.Response orginalResponse = chain.proceed(chain.request());
            Response response = orginalResponse.newBuilder()
                    .body(new ProgressResponseBody(orginalResponse.body(), listener, upFilePath))
                    .build();
            return response;
        }
    }

    //网络请求拦截（日志打印）
    public class Network implements Interceptor {
        Charset UTF8 = Charset.forName("UTF-8");

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Headers headers = request.headers();
            Set<String> names = headers.names();
            String headMap = "";
            if (names != null && names.size() > 0) {
                Iterator<String> values = names.iterator();
                while (values.hasNext()) {
                    String key = values.next();
                    String value = headers.get(key);
                    headMap += " " + key + ":" + value + " ;";
                }
            }
            //
            String body = "";
            RequestBody requestBody = request.body();
            if (requestBody != null) {
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);
                Charset charset = UTF8;
                MediaType contentType = requestBody.contentType();
                if (contentType != null) {
                    charset = contentType.charset(UTF8);
                }
                body = buffer.readString(charset);
            }
            //继续前前进（开始请求）
            long startTime = System.currentTimeMillis();
            Response response = chain.proceed(request);
            //
            String res = "";
            ResponseBody responseBody = response.body();
            MediaType contentType = responseBody.contentType();
            if (contentType != null) {
                Charset charset = UTF8;
                try {
                    charset = contentType.charset(UTF8);
                } catch (UnsupportedCharsetException e) {
                    e.printStackTrace();
                }
                BufferedSource source = responseBody.source();
                source.request(Long.MAX_VALUE);
                Buffer buffer = source.buffer();
                res = buffer.clone().readString(charset);
            }
            long time = System.currentTimeMillis() - startTime;
            RLog.e("--", "\n响应: code:" + response.code()
                    + "\nurl：" + response.request().url()
                    + "\nheads:" + headMap
                    + "\n请求：" + body
                    + "\n返回: " + res
                    + "\n响应时间：" + time + "毫秒");

            return response;
        }
    }
}
