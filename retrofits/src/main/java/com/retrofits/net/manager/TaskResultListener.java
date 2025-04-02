package com.retrofits.net.manager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.retrofits.net.manager.BaseManager.WHAT_LOCALITY_NET_WORK_ERROR;

/**
 * Created by Administrator on 2017/6/14.
 */

public abstract class TaskResultListener<T> implements Callback<T> {

    public String other;
    protected BaseManager baseManager;

    public TaskResultListener(BaseManager baseManager) {
        this.baseManager = baseManager;
    }

    public TaskResultListener(BaseManager baseManager, Object reqObj) {
        this.baseManager = baseManager;

    }

    public TaskResultListener(BaseManager baseManager, Object reqObj, String other) {
        this.baseManager = baseManager;
        this.other = other;
    }

    public TaskResultListener(BaseManager baseManager, String other) {
        this.baseManager = baseManager;
        this.other = other;
    }

    public abstract void onRequestResult(Call<T> call, Response<T> response);


    public void setOther(String other) {
        this.other = other;
    }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        onRequestResult(call, response);
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        String msg = t.toString();
        String m = msg;
        if (msg.contains("TimeoutException")) {
            //m = "请求超时";
            m = "网络出小差，请稍后重试";
        }
        if (msg.contains("Failed to connect to")) {
            m = "无法连接服务器";
        }
        if (msg.contains("No address associated with hostname")) {
            m = "网络连接失败";
        }
        if (msg.contains("JsonParseException")) {
            m = "数据解析失败";
        }
        if (msg.contains("Socket closed")) {
            m = "已断开连接";
        }
        baseManager.onBack(onDealFailed(WHAT_LOCALITY_NET_WORK_ERROR, ""), null, m, other, false);
    }



    public Object getObject(Response<T> response) {
        return response;
    }

    public int onDealSucceed(int what) {
        return what;
    }

    public int onDealFailed(int what, String code) {
        return what;
    }
}
