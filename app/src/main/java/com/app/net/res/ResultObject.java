package com.app.net.res;

/**
 * Created by Administrator on 2016/4/8.
 */
public class ResultObject<T> extends BaseResult {
    private T obj;

    public T getObj() {
        return obj;
    }

    public void setObj(T obj) {
        this.obj = obj;
    }
}
