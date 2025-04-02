package com.retrofits.net.common.custom;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.retrofits.utiles.RLog;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Created by Administrator on 2017/5/15.
 */

public class JacksonFactory extends Converter.Factory {
    /**
     * Create an instance using a default {@link ObjectMapper} instance for conversion.
     */
    public static JacksonFactory create() {
        return create(new ObjectMapper());
    }

    /**
     * Create an instance using {@code mapper} for conversion.
     */
    public static JacksonFactory create(ObjectMapper mapper) {
        return new JacksonFactory(mapper);
    }

    private final ObjectMapper mapper;

    private JacksonFactory(ObjectMapper mapper) {
        if (mapper == null) throw new NullPointerException("mapper == null");
        this.mapper = mapper;
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
                                                            Retrofit retrofit) {
        JavaType javaType = mapper.getTypeFactory().constructType(type);
        ObjectReader reader = mapper.reader(javaType);
        String className = javaType.getRawClass().getName();
        boolean isStrClas = className.equals(String.class.getName());
        RLog.e("responseBodyConverter", "className:" + className + " isStrClas:" + isStrClas);
        return new JacksonResponseBody<>(reader, isStrClas);
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type,
                                                          Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        JavaType javaType = mapper.getTypeFactory().constructType(type);
        ObjectWriter writer = mapper.writerWithType(javaType);
        return new JacksonRequestBody<>(writer);
    }
}
