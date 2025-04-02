package com.retrofits.net.common;

import android.content.Context;

/**
 * Created by Administrator on 2016/9/7.
 */
public abstract class BaseUrl {
    //获取baseUrl
    public abstract String getUrl();

    public abstract Context getContext();

    //cer 没有密码 传null
    //获取在assets里的证书路径 ["路径",密码,"路径",密码]
    public String[] getSSLCertificates() {
        return null;
    }

    public String[] getHostName() {
        return null;
    }

    //是否开启使用证书
    public abstract boolean isSSL();
}
