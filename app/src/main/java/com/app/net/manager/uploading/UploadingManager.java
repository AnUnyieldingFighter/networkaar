package com.app.net.manager.uploading;

import android.content.Context;
import android.util.Log;

import com.app.net.common.RequestResultListener;
import com.app.net.common.RequestResultThreadListener;
import com.app.net.common.UrlManger;
import com.app.net.req.UploadingBeanReq;
import com.app.net.res.ResultObject;
import com.retrofits.net.common.BaseNetSource;
import com.retrofits.net.common.RequestBack;
import com.retrofits.net.common.body.RequestBodyProUpload;
import com.retrofits.net.manager.BaseManager;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;


/**
 * 上传文件
 * Created by Administrator on 2016/9/7.
 */
public class UploadingManager extends BaseManager {

    public UploadingManager(RequestBack requestBack) {
        super(requestBack);

    }


    //上传文件 方式1：setProgressListener(1, getProgress(false), file.getName());
    public void request(File file) {
        RequestBody requestFile =
                RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part body =
                MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        //
        BaseNetSource source = new BaseNetSource();
        //
        source.setProgressListener(1, getProgress(false), file.getName());
        Retrofit retrofit = source.getRetrofit(new UrlManger());
        //
        UpApi service = retrofit.create(UpApi.class);
        //
        UploadingBeanReq req = new UploadingBeanReq();
        RequestBody serviceReq = RequestBody.create(null, req.service);
        RequestBody spid = RequestBody.create(null, req.spid);
        RequestBody oper = RequestBody.create(null, req.oper);
        RequestBody channel = RequestBody.create(null, req.channel);
        RequestBody random = RequestBody.create(null, req.random);
        String sing = "";
        RequestBody sign = RequestBody.create(null, sing);
        //
        Call<ResultObject<String>> call = service.uploading(serviceReq, spid, oper, channel, random, sign, body);
        RequestResultThreadListener<ResultObject<String>> listener = new RequestResultThreadListener<ResultObject<String>>(this, call) {
            @Override
            public Object getObject(Response<ResultObject<String>> response) {
                ResultObject<String> body = response.body();
                String obj = body.getObj();
                return obj;
            }
        };
        listener.setOther(file.getName());
        listener.start();
    }


    //上传文件（直接使用 RequestBodyProUpload 作为参数上传）
    public void request21(File file) {
        //MediaType.parse("video/mp4")
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), file);
        request21(requestBody, file.getPath());
    }

    //上传文件
    public void request21(RequestBody requestBody, String filePath) {
        // 3. 包装成带进度的 RequestBody
        RequestBodyProUpload progressBody = new RequestBodyProUpload(requestBody, getProgress(), filePath);
        request22(progressBody, filePath);
    }
    //上传文件 方式2： source.setProgressType(1); 但是没有设置监听，监听会走 RequestBodyProUpload
    private void request22(RequestBodyProUpload progressBody, String other) {
        // 4. 丢给 Retrofit 上传
        BaseNetSource source = new BaseNetSource();
        source.setProgressType(1);
        Retrofit retrofit = source.getRetrofit(new UrlManger());
        UpApi service = retrofit.create(UpApi.class);
        Call<ResultObject<String>> call = service.uploadRB(progressBody);
        call.enqueue(new RequestResultListener<ResultObject<String>>(this, other) {
            @Override
            public Object getObject(Response<ResultObject<String>> response) {
                ResultObject<String> body = response.body();
                String obj = body.getObj();
                return obj;
            }

            @Override
            public void onResponse(Call<ResultObject<String>> call, Response<ResultObject<String>> response) {
                super.onResponse(call, response);
                if (response.isSuccessful()) {
                    //完成
                    //listener.onProgress(3, "成功", path, total, total);
                } else {
                    //服务器错误
                    getProgress().onProgress(4, "", other, -1, -1, "Server Error");
                }
                getProgress();
            }

            @Override
            public void onFailure(Call<ResultObject<String>> call, Throwable e) {
                super.onFailure(call, e);
                //错误 也会走这里
                // call.cancel();   会走这里
                //1：开始 2：进行中 3：完成 4：出错 5:停止下载
                if ("Canceled".equals(e.getMessage()) || call.isCanceled()) {
                    //  取消
                    getProgress().onProgress(5, "", other, -1, -1, e.getMessage());
                } else {
                    // 其他失败
                    getProgress().onProgress(4, "", other, -1, -1, e.getMessage());

                }
            }
        });

    }


}
