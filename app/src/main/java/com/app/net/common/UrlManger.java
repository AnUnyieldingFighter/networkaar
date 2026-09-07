package com.app.net.common;

import android.content.Context;

import com.app.ui.activitty.MainApplication;
import com.retrofits.net.common.BaseUrl;

/**
 * Created by Administrator on 2017/6/14.
 */

public class UrlManger extends BaseUrl{

    @Override
    public String getUrl() {
        return "https://api-djy.djbx.com/app/";
    }

    @Override
    public Context getContext() {
        return MainApplication.context;
    }

    @Override
    public String[] getSSLCertificates() {
          return new String[]{"djy_djbx.com.cer"};
        // return null;
    }

    @Override
    public boolean isSSL() {
        return true;
    }

    @Override
    public boolean isTrustAllCertificates() {
        // 正式环境必须保持 false；只有受控测试环境排查证书问题时才改为 true。
        return false;
    }
}
