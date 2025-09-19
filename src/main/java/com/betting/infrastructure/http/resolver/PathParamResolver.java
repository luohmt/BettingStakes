package com.betting.infrastructure.http.resolver;

import com.sun.net.httpserver.HttpExchange;
import com.betting.infrastructure.http.router.PathParam;
import com.betting.infrastructure.http.router.TypeConverter;

import java.lang.reflect.Parameter;
import java.util.Map;

public class PathParamResolver implements ParamResolver {
    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(PathParam.class);
    }

    @Override
    public Object resolve(Parameter parameter,
                          HttpExchange exchange,
                          Map<String, String> pathParams,
                          Map<String, String> queryParams,
                          String body) {
        PathParam ann = parameter.getAnnotation(PathParam.class);
        String raw = pathParams.get(ann.value());
        return TypeConverter.convert(raw, parameter.getType());
    }
}
