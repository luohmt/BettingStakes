package com.betting.infrastructure.http.resolver;

import com.betting.infrastructure.http.router.BodyParam;
import com.betting.infrastructure.http.router.TypeConverter;
import com.betting.util.LightJsonParser;
import com.sun.net.httpserver.HttpExchange;

import java.lang.reflect.Parameter;
import java.util.Map;

public class BodyParamResolver implements ParamResolver {
    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(BodyParam.class);
    }

    @Override
    public Object resolve(Parameter parameter,
                          HttpExchange exchange,
                          Map<String, String> pathParams,
                          Map<String, String> queryParams,
                          String body) {
        BodyParam ann = parameter.getAnnotation(BodyParam.class);
        if (ann.value().isEmpty() || body == null) {
            return body;
        }
        String raw = extractJsonValue(body, ann.value());
        return TypeConverter.convert(raw, parameter.getType());
    }

    private String extractJsonValue(String json, String key) {
        if (json == null) return null;
        LightJsonParser parser = new LightJsonParser(json);
        String body = parser.getString(key);
        return body != null ? body : null;
    }


}
