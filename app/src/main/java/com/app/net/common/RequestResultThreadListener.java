package com.app.net.common;

import android.text.TextUtils;

import com.app.net.res.BaseResult;
import com.retrofits.net.manager.BaseManager;
import com.retrofits.net.manager.TaskResultThreadListener;

import retrofit2.Call;
import retrofit2.Response;

import static com.retrofits.net.manager.BaseManager.WHAT_DEAL_FAILED;
import static com.retrofits.net.manager.BaseManager.WHAT_DEAL_SUCCEED;

/**
 * Created by Administrator on 2017/6/14.
 */

public class RequestResultThreadListener<T> extends TaskResultThreadListener<T> {
    public RequestResultThreadListener(BaseManager baseManager, Call<T> call) {
        super(call);
        this.baseManager = baseManager;
    }

    public RequestResultThreadListener(BaseManager baseManager, Call<T> call, Object reqObj) {
        super(call, reqObj);
        this.baseManager = baseManager;
    }

    @Override
    public void onRequestResult(Call<T> call, Response<T> response) {
        T body = response.body();
        BaseResult baseResult = null;
        if (body instanceof BaseResult) {
            baseResult = (BaseResult) body;
        }
        Object obj = null;
        String responseMsg = "";
        String code = "";
        //
        if (baseResult == null) {
            responseMsg = "没有返回数据";
            code = "-1";
        } else {
            responseMsg = baseResult.msg;
            code = baseResult.code;
            obj = getObject(response);
        }
        if ("0".equals(code) || TextUtils.isEmpty(code)) {
            //业务请求成功
            baseManager.onBack(onDealSucceed(WHAT_DEAL_SUCCEED), obj, responseMsg, other, true);
            return;
        }
        //业务请求 其他原因失败
        baseManager.onBack(onDealFailed(WHAT_DEAL_FAILED, code), obj, responseMsg, other, true);

    }
}
