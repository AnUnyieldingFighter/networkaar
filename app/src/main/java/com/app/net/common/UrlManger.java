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
}
