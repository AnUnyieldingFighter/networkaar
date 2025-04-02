package com.retrofits.net.manager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.retrofits.net.common.ProgressListener;
import com.retrofits.net.common.RequestBack;
import com.retrofits.utiles.RLog;

import java.lang.ref.SoftReference;

/**
 * Created by Administrator on 2016/10/20.
 */
public abstract class BaseManager {

    /**
     * 网络错误
     */
    public static final int WHAT_LOCALITY_NET_WORK_ERROR = 201;
    /**
     * 业务处理成功
     */
    public static final int WHAT_DEAL_SUCCEED = 300;
    /**
     * 业务操作失败
     */
    public static final int WHAT_DEAL_FAILED = 301;
    /**
     * 进度
     */
    public static final int WHAT_LOADING_PROGRESS = 299;
    protected HandleCall handleCall = new HandleCall();
    //使用方法 BaseNetSource.setProgressListener(progress);


    SoftReference<RequestBack> requestBacks = null;

    public BaseManager(RequestBack requestBack) {
        if (requestBack == null) {
            return;
        }
        requestBacks = new SoftReference<RequestBack>(requestBack);
    }

    private RequestBack getRequestBack() {
        if (requestBacks == null) {
            return null;
        }
        RequestBack requestBack = requestBacks.get();
        return requestBack;
    }

    public Handler getHandleCall() {
        return handleCall;
    }

    public void onBack(int code, Object obj, String msg, String other, boolean isExchange) {
        if (isExchange) {
            Message message = handleCall.obtainMessage();
            message.obj = obj;
            message.what = -200;
            Bundle bundle = new Bundle();
            bundle.putInt("code", code);
            bundle.putString("msg", msg);
            bundle.putString("other", other);
            message.setData(bundle);
            handleCall.sendMessage(message);
            return;
        }
        RequestBack requestBack = getRequestBack();
        if (requestBack == null) {
            return;
        }
        requestBack.onBack(code, obj, msg, other);
    }

    //网络请求的handle
    protected void netHandleMessage(Message msg) {
        //-200 :请求 -201：进度
        int what = msg.what;
        RequestBack requestBack = getRequestBack();
        if (requestBack == null) {
            return;
        }
        switch (what) {
            case -200:
                //api请求成功
                Object obj = msg.obj;
                Bundle bundle = msg.getData();
                int code = bundle.getInt("code", -1);
                String hint = bundle.getString("msg");
                String other = bundle.getString("other");
                requestBack.onBack(code, obj, hint, other);
                break;
            case -201:
                //进度
                bundle = msg.getData();
                int whatCode = bundle.getInt("what", -1);
                String url = bundle.getString("url");
                String filePath = bundle.getString("filePath");
                long progress = bundle.getLong("progress", 0);
                long total = bundle.getLong("total", 0);
                boolean isOnBack = bundle.getBoolean("isOnBack", false);
                if (!isOnBack) {
                    requestBack.onBackProgress(whatCode, url, filePath, progress, total);
                } else {
                    requestBack.onBack(WHAT_LOADING_PROGRESS, whatCode == 3, String.valueOf(progress), String.valueOf(total));
                }
                break;
        }


    }


    class HandleCall extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            netHandleMessage(msg);
        }
    }

    private Progress progress;

    public Progress getProgress() {
        return getProgress(false);
    }

    public Progress getProgress(boolean isOnBack) {
        if (progress == null) {
            progress = new Progress(isOnBack);
        }
        return progress;
    }

    //上传下载进度条
    public class Progress implements ProgressListener {
        //true 发送到 OnBack;
        private boolean isOnBack;

        public Progress(boolean isOnBack) {
            this.isOnBack = isOnBack;
        }

        /**
         * 1：开始 2：进行中 3：完成 4：出错 5 停止
         *
         * @param url      下载的url
         * @param filePath 本地保存的path或者上传时的文件path
         * @param progress 已经下载或上传字节数
         * @param total    总字节数
         */
        @Override
        public void onProgress(int what, String url, String filePath, long progress, long total) {
            RLog.e("BaseManager", "进度：progress：" + progress + " total:" + total);
            Message message = handleCall.obtainMessage();
            message.what = -201;
            Bundle bundle = new Bundle();
            bundle.putInt("what", what);
            bundle.putString("url", url);
            bundle.putString("filePath", filePath);
            bundle.putLong("progress", progress);
            bundle.putLong("total", total);
            bundle.putBoolean("isOnBack", isOnBack);
            message.setData(bundle);
            handleCall.sendMessage(message);
        }
    }
}
