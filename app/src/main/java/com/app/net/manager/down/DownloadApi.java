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
    @GET
    Call<ResponseBody> download(@Header("Range") String start, @Url String fileUrl);

    @GET
    Call<ResponseBody> download(@Url String fileUrl);

}

