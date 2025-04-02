package com.retrofits.downsys;

/**
 * 系统下载服务
 * Created by guom on 2019/1/4.
 */
public abstract class SysDownloadServise extends BaseSysDownloadServise {

    @Override
    public void onCreate() {
        register();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unRegister();
    }

}

