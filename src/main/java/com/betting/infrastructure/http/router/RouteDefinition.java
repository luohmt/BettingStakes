package com.betting.infrastructure.http.router;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class RouteDefinition {
    
    /**
     * Regex pattern for path parameters
     */
    private static final String PATH_PARAM_REGEX = "([^/]+)";
    
    /**
     * Path separator
     */
    private static final String PATH_SEPARATOR = "/";
    
    /**
     * Start of path parameter
     */
    private static final String PARAM_START = "{";
    
    /**
     * End of path parameter
     */
    private static final String PARAM_END = "}";
    
    /**
     * Regex anchors for complete path matching
     */
    private static final String REGEX_START = "^";
    private static final String REGEX_END = "/?$";
    
    private final String method;
    private final Pattern pathPattern;
    private final List<String> pathParamNames;
    private final Object controller;
    private final Method methodRef;

    public RouteDefinition(String method, String pathPattern, Object controller, Method methodRef) {
        this.method = method.toUpperCase();
        this.controller = controller;
        this.methodRef = methodRef;
        this.pathParamNames = new ArrayList<>();

        String regex = Arrays.stream(pathPattern.split(PATH_SEPARATOR))
                .map(s -> {
                    if (s.startsWith(PARAM_START) && s.endsWith(PARAM_END)) {
                        pathParamNames.add(s.substring(1, s.length() - 1));
                        return PATH_PARAM_REGEX;
                    } else {
                        return Pattern.quote(s);
                    }
                })
                .reduce((a, b) -> a + PATH_SEPARATOR + b)
                .orElse("");
        this.pathPattern = Pattern.compile(REGEX_START + regex + REGEX_END);
    }

    public boolean matches(String requestMethod, String path, Map<String, String> pathParams) {
        if (!method.equals(requestMethod.toUpperCase())) return false;
        Matcher matcher = pathPattern.matcher(path);
        if (!matcher.matches()) return false;
        for (int i = 0; i < pathParamNames.size(); i++) {
            pathParams.put(pathParamNames.get(i), matcher.group(i + 1));
        }
        return true;
    }

    public Object getController() {
        return controller;
    }

    public Method getMethodRef() {
        return methodRef;
    }
}



