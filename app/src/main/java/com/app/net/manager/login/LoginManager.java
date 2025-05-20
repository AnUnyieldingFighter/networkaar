package com.app.net.manager.login;


import com.app.net.common.RequestResultListener;
import com.app.net.common.UrlManger;
import com.app.net.req.LoginBeanReq;
import com.app.net.res.ResultObject;
import com.app.net.res.SysUser;
import com.retrofits.net.common.BaseNetSource;
import com.retrofits.net.common.RequestBack;
import com.retrofits.net.manager.BaseManager;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Response;


/**
 * Created by Administrator on 2016/9/7.
 */
public class LoginManager extends BaseManager {
    public LoginManager(RequestBack requestBack) {
        super(requestBack);
    }

    private LoginBeanReq loginBeanReq;

    public void setData(String account, String password) {
        if (loginBeanReq == null) {
            loginBeanReq = new LoginBeanReq();
        }
        loginBeanReq.docMobile = account;
        loginBeanReq.docPassword = password;
    }


    public void request() {
        HashMap<String, String> map = new HashMap();
        map.put("sign", "test");
        LoginApi service = new BaseNetSource().getRetrofit(new UrlManger()).create(LoginApi.class);
        Call<ResultObject<SysUser>>  call = service.loginIn(map, loginBeanReq);
        call.enqueue(new RequestResultListener<ResultObject<SysUser>>(this, loginBeanReq) {
            @Override
            public Object getObject(Response<ResultObject<SysUser>> response) {
                ResultObject<SysUser> body = response.body();
                SysUser obj = body.getObj();
                return obj;
            }
        });
    }
}
