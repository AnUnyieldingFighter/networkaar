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
    //[0] 服务端证书路径      App 用来验证服务器的证书/信任库
    //[1] 服务端 BKS 密码   对应密码
    //[2] 客户端证书路径    App 向服务器出示的客户端证书
    //[3] 客户端 BKS 密码   // 对应密码
    public String[] getSSLCertificates() {
        return null;
    }

    public String[] getHostName() {
        return null;
    }

    /**
     * 是否信任所有 HTTPS 证书和主机名。
     * 默认关闭；该模式会失去 HTTPS 身份校验能力，只能用于受控测试环境。
     */
    public boolean isTrustAllCertificates() {
        return true;
    }

    //是否开启使用证书
    public abstract boolean isSSL();

    //true 打印请求->返回数据用时
    public boolean isReqTimeContinue() {
        return false;
    }
}
