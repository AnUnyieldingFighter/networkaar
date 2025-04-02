package com.retrofits.net.common.custom;

import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.io.Reader;

import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * Created by Administrator on 2017/5/15.
 */

public class JacksonResponseBody<T> implements Converter<ResponseBody, T> {
    private final ObjectReader adapter;
    private boolean isReturn;

    JacksonResponseBody(ObjectReader adapter, boolean isReturn) {
        this.adapter = adapter;
        this.isReturn = isReturn;
    }

    @Override
    public T convert(ResponseBody value) throws IOException {
        if (isReturn) {
            String str = value.string();
            value.close();
            return (T) str;
        }
        try {
            Reader r = value.charStream();
            return adapter.readValue(r);
        } finally {
            value.close();
        }
    }
}
