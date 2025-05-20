package com.app.net.manager.uploading;

import com.app.net.common.RequestResultThreadListener;
import com.app.net.common.UrlManger;
import com.app.net.req.UploadingBeanReq;
import com.app.net.res.ResultObject;
import com.retrofits.net.common.BaseNetSource;
import com.retrofits.net.common.RequestBack;
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
        source.setProgressListener(getProgress(false), file.getName());
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
        String sing="";
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

}
