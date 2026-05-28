package com.app.net.manager.uploading;

import com.app.net.res.ResultObject;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;


/**
 * Created by Administrator on 2016/9/7.
 */
public interface UpApi {

    //上传文件
    @Multipart
    @POST("app/")
    Call<ResultObject<String>> uploading(
            @Part("service") RequestBody service,
            @Part("spid") RequestBody spid,
            @Part("oper") RequestBody oper,
            @Part("channel") RequestBody channel,
            @Part("random") RequestBody random,
            @Part("sign") RequestBody sign,
            @Part MultipartBody.Part file);


    // 单文件上传：直接传 RequestBody
    @POST("http://10.168.3.72/nbc-api/file/upd/video")
    Call<ResultObject<String>> uploadRB(@Body RequestBody requestBod);
    // 单文件上传： 表单上传
    @Multipart
    @POST("http://10.168.3.72/nbc-api/file/upd/video")
    Call<ResultObject<String>> uploadMB(@Part MultipartBody.Part part);


}

