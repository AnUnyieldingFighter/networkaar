package com.retrofits.net.manager;

import com.retrofits.net.common.thread.NetSourceThreadPool;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.retrofits.net.manager.BaseManager.WHAT_LOCALITY_NET_WORK_ERROR;

/**
 * Created by Administrator on 2017/6/14.
 */

public abstract class TaskResultThreadListener<T> implements Callback<T>, Runnable {

    public String other;
    protected BaseManager baseManager;
    private Call<T> call;

    public TaskResultThreadListener(Call<T> call) {
        this.call = call;
    }

    public TaskResultThreadListener(Call<T> call, Object reqObj) {
        this.call = call;

    }

    public TaskResultThreadListener(Call<T> call, String other, Object reqObj) {
        this.call = call;
        this.other = other;

    }

    public void setOther(String other) {
        this.other = other;
    }
    protected void onStop(Call<T> call) {
        if (call == null) {
            return;
        }
        if (call.isCanceled()) {
            return;
        }
        call.cancel();
    }

    public abstract void onRequestResult(Call<T> call, Response<T> response);

    public void start() {
        NetSourceThreadPool.getInstance().execute(this);
    }

    public void start(String key) {
        NetSourceThreadPool.getInstance().submit(key, this);
    }

    @Override
    public void run() {
        try {
            Response<T> response = call.execute();
            onResponse(call, response);
        } catch (IOException e) {
            e.printStackTrace();
            onFailure(call, e);
        }
    }

    public void onResponse(Call<T> call, Response<T> response) {
        onRequestResult(call, response);
    }

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
        baseManager.onBack(onDealFailed(WHAT_LOCALITY_NET_WORK_ERROR, ""), null, m, other, true);
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
