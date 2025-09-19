package com.betting.infrastructure.http.resolver;


import com.sun.net.httpserver.HttpExchange;

import java.lang.reflect.Parameter;
import java.util.Map;

public interface ParamResolver {
    boolean supports(Parameter parameter);

    Object resolve(Parameter parameter,
                   HttpExchange exchange,
                   Map<String, String> pathParams,
                   Map<String, String> queryParams,
                   String body);
}
