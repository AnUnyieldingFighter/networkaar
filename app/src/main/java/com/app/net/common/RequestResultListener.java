package com.app.net.common;

import android.text.TextUtils;

import com.app.net.res.BaseResult;
import com.retrofits.net.manager.BaseManager;
import com.retrofits.net.manager.TaskResultListener;

import retrofit2.Call;
import retrofit2.Response;

import static com.retrofits.net.manager.BaseManager.WHAT_DEAL_FAILED;
import static com.retrofits.net.manager.BaseManager.WHAT_DEAL_SUCCEED;

/**
 * Created by Administrator on 2017/6/14.
 */

public class RequestResultListener<T> extends TaskResultListener<T> {
    public RequestResultListener(BaseManager baseManager) {
        super(baseManager);
    }

    public RequestResultListener(BaseManager baseManager, Object reqObj) {
        super(baseManager, reqObj);
    }

    public RequestResultListener(BaseManager baseManager, Object reqObj, String other) {
        super(baseManager, reqObj, other);
    }

    public RequestResultListener(BaseManager baseManager, String other) {
        super(baseManager, other);
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
            baseManager.onBack(onDealSucceed(WHAT_DEAL_SUCCEED), obj, responseMsg, other, false);
            return;
        }
        //业务请求 其他原因失败
        baseManager.onBack(onDealFailed(WHAT_DEAL_FAILED, code), obj, responseMsg, other, false);
    }
}
