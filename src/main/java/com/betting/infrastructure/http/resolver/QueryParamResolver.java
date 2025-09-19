package com.betting.infrastructure.http.resolver;

import com.sun.net.httpserver.HttpExchange;
import com.betting.infrastructure.http.router.QueryParam;
import com.betting.infrastructure.http.router.TypeConverter;

import java.lang.reflect.Parameter;
import java.util.Map;

public class QueryParamResolver implements ParamResolver {
    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(QueryParam.class);
    }

    @Override
    public Object resolve(Parameter parameter,
                          HttpExchange exchange,
                          Map<String, String> pathParams,
                          Map<String, String> queryParams,
                          String body) {
        QueryParam ann = parameter.getAnnotation(QueryParam.class);
        String raw = queryParams.get(ann.value());
        return TypeConverter.convert(raw, parameter.getType());
    }
}
