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

    private File file;

    public void setData(File file) {
        this.file = file;
    }


    public void request() {
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


    //上传文件
    public void request21(File file) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), file);
        request21(requestBody, file.getPath());
    }

    //上传文件
    public void request21(RequestBody requestBody, String fileName) {
        // 3. 包装成带进度的 RequestBody
        RequestBodyProUpload progressBody = new RequestBodyProUpload(requestBody, getProgress(), fileName);
        request22(progressBody);
    }

    private void request22(RequestBodyProUpload progressBody) {
        // 4. 丢给 Retrofit 上传
        BaseNetSource source = new BaseNetSource();
        source.setProgressType(1);
        Retrofit retrofit = source.getRetrofit(new UrlManger());
        UpApi service = retrofit.create(UpApi.class);
        Call<ResultObject<String>> call = service.uploadRB(progressBody);
        call.enqueue(new RequestResultListener<ResultObject<String>>(this) {
            @Override
            public Object getObject(Response<ResultObject<String>> response) {
                ResultObject<String> body = response.body();
                String obj = body.getObj();
                return obj;
            }
        });
    }

    //上传表单
    public void request3() {
        // 1. 你要上传的文件
        File file = new File("路径/xxx.jpg");
        // 2. 创建普通 RequestBody
        RequestBody requestBody =
                RequestBody.create(MediaType.parse("video/mp4"), file);
        // 3. 包装成带进度的 RequestBody
        RequestBodyProUpload progressBody = new RequestBodyProUpload(requestBody, getProgress(), file.getPath());
        //表单上传 包装 progressBody
        MultipartBody.Part mBody =
                MultipartBody.Part.createFormData("file", file.getName(), progressBody);
        // 4. 丢给 Retrofit 上传
        BaseNetSource source = new BaseNetSource();
        Retrofit retrofit = source.getRetrofit(new UrlManger());
        source.setProgressType(1);
        UpApi service = retrofit.create(UpApi.class);
        Call<ResultObject<String>> call = service.uploadMB(mBody);
        call.enqueue(new RequestResultListener<ResultObject<String>>(this) {
            @Override
            public Object getObject(Response<ResultObject<String>> response) {
                ResultObject<String> body = response.body();
                String obj = body.getObj();
                return obj;
            }
        });
    }
}
