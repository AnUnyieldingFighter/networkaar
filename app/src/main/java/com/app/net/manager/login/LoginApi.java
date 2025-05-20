package com.app.net.manager.login;

import com.app.net.req.LoginBeanReq;
import com.app.net.res.ResultObject;
import com.app.net.res.SysUser;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;


/**
 * Created by Administrator on 2016/9/7.
 */
public interface LoginApi {

    @POST("./")
    Call<ResultObject<SysUser>> loginIn(@HeaderMap Map<String, String> map, @Body LoginBeanReq resetPushBean);


}

