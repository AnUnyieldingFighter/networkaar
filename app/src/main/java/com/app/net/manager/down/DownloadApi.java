package com.app.net.manager.down;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Streaming;
import retrofit2.http.Url;


/**
 * Created by Administrator on 2016/9/7.
 */
public interface DownloadApi {
    //@Header("RANGE") String start 断点下载开始位置(bytes=" + 536 + "-")
    @Streaming/*大文件需要加入这个判断，防止下载过程中写入到内存中*/
    @GET   //Range 解决“从哪里继续下载”，If-Range 解决“还能不能继续下载同一个文件
    Call<ResponseBody> download(
            //指定从哪个位置继续下载
            @Header("Range") String start,
            //如果服务器上的文件还是上次那个文件，
            //就从断点继续；如果文件已经变化，就重新返回完整文件。
            //返回 206：文件没变，可以继续追加。
            //返回 200：服务器没有续传，或者文件已经变化，需要从头覆盖下载。
            //返回 416：请求的起点超过文件长度，可能临时文件已经完整，也可能本地进度无效。
            @Header("If-Range") String validator,
            @Url String fileUrl);

    @Streaming // 下载内容直接写入文件，禁止 Retrofit 将整个响应读入内存。
    @GET
    Call<ResponseBody> download(@Url String fileUrl);


    @Streaming // 带进度的下载同样必须使用流式响应，避免大文件导致内存溢出。
    @GET
    Call<ResponseBody> download2(@Url String url);

}

