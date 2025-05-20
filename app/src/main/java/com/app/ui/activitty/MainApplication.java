package com.app.ui.activitty;

import android.app.Application;
import android.content.Context;

/**
 * Created by Administrator on 2016/9/7.
 */
public class MainApplication extends Application {
    public static MainApplication application;
    public static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        context = this;

    }




}
