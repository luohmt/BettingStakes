package com.betting.infrastructure.http.router;

import com.betting.api.exception.BettingException;
import com.betting.infrastructure.http.interceptor.RequestInterceptor;
import com.betting.infrastructure.http.resolver.ParamResolver;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * HTTP Router Implementation
 * <p>
 * This router provides a lightweight, annotation-based routing system for HTTP requests.
 * It supports path parameters, query parameters, request body parameters, and request interceptors.
 * <p>
 * Key Features:
 * - Annotation-based route registration (@Route)
 * - Automatic parameter resolution using ParamResolver chain
 * - Request/response interceptors for cross-cutting concerns
 * - Path parameter extraction with regex matching
 * - Query parameter parsing
 * - Request body reading and parsing
 */
public class Router {

    // HTTP Status Codes
    private static final int HTTP_OK = 200;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_INTERNAL_SERVER_ERROR = 500;

    // HTTP Headers
    public static final String CONTENT_LENGTH_HEADER = "Content-Length";

    // Default values
    private static final String DEFAULT_CONTENT_LENGTH = "0";
    public static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal Server Error";
    public static final String NOT_FOUND_MESSAGE = "Not Found";

    // Query parameter parsing
    public static final String QUERY_PARAM_SEPARATOR = "&";
    public static final String QUERY_KEY_VALUE_SEPARATOR = "=";
    public static final int MAX_KEY_VALUE_PARTS = 2;

    /**
     * Registered route definitions
     * Each route contains method, path pattern, controller, and method reference
     */
    private final List<RouteDefinition> routes = new ArrayList<>();

    /**
     * Parameter resolvers for different parameter types
     * Used to resolve method parameters from HTTP request data
     */
    private final List<ParamResolver> resolvers;

    /**
     * Request interceptors for cross-cutting concerns
     * Applied to all requests for logging, authentication, etc.
     */
    private final List<RequestInterceptor> interceptors = new ArrayList<>();
    private final RequestExtractor extractor = new RequestExtractor();

    /**
     * Creates a new router with the specified parameter resolvers
     *
     * @param resolvers List of parameter resolvers for different parameter types
     */
    public Router(List<ParamResolver> resolvers) {
        this.resolvers = resolvers;
    }


    /**
     * Registers multiple request interceptors
     * <p>
     * Interceptors are applied to all requests in the order they are registered.
     * They can be used for logging, authentication, rate limiting, etc.
     *
     * @param interceptors Array of request interceptors to register
     */
    public void addInterceptors(RequestInterceptor... interceptors) {
        this.interceptors.addAll(Arrays.asList(interceptors));
    }

    /**
     * Registers multiple controllers
     * <p>
     * This is a convenience method for registering multiple controllers at once.
     * Each controller will be scanned for @Route annotated methods.
     *
     * @param controllers Array of controller objects to register
     */
    public void registerControllers(Object... controllers) {
        Arrays.stream(controllers).forEach(this::registerController);
    }

    /**
     * Registers a single controller
     * <p>
     * Scans the controller class for methods annotated with @Route and creates
     * route definitions for each found method.
     *
     * @param controller The controller object to register
     */
    public void registerController(Object controller) {
        for (Method method : controller.getClass().getDeclaredMethods()) {
            Route route = method.getAnnotation(Route.class);
            if (route != null) {
                routes.add(new RouteDefinition(route.method(), route.path(), controller, method));
            }
        }
    }

    /**
     * Routes an HTTP request to the appropriate controller method
     *
     * @param exchange the HTTP exchange containing request and response
     * @throws IOException if there's an I/O error during processing
     */
    public void route(HttpExchange exchange) throws IOException {
        RequestContext context = extractor.extractRequestContext(exchange);

        Optional<RouteDefinition> matchingRoute = findMatchingRoute(context);

        if (matchingRoute.isPresent()) {
            handleRequest(exchange, matchingRoute.get(), context);
        } else {
            sendNotFoundResponse(exchange);
        }
    }

    /**
     * Finds a route that matches the request context
     *
     * @param context the request context
     * @return optional route definition if found
     */
    private Optional<RouteDefinition> findMatchingRoute(RequestContext context) {
        return routes.stream()
                .filter(route -> route.matches(context.method(), context.path(), context.pathParams()))
                .findFirst();
    }

    /**
     * Handles the request by invoking the appropriate controller method
     *
     * @param exchange the HTTP exchange
     * @param route    the matching route definition
     * @param context  the request context
     * @throws IOException if there's an I/O error
     */
    private void handleRequest(HttpExchange exchange, RouteDefinition route, RequestContext context) throws IOException {
        try {
            executePreHandlers(exchange);

            Object[] methodArgs = resolveMethodParameters(route.getMethodRef(), exchange, context);
            Object result = invokeControllerMethod(route, methodArgs);

            executePostHandlers(exchange, result);
            ResponseWriter.write(exchange, result);

        } catch (Exception e) {
            handleRequestException(exchange, e);
        }
    }

    /**
     * Executes pre-request interceptors
     *
     * @param exchange the HTTP exchange
     */
    private void executePreHandlers(HttpExchange exchange) {
        interceptors.forEach(interceptor -> interceptor.preHandle(exchange));
    }

    /**
     * Executes post-request interceptors
     *
     * @param exchange the HTTP exchange
     * @param result   the method execution result
     */
    private void executePostHandlers(HttpExchange exchange, Object result) {
        interceptors.forEach(interceptor -> interceptor.postHandle(exchange, result));
    }

    /**
     * Invokes the controller method with resolved parameters
     *
     * @param route      the route definition
     * @param methodArgs the resolved method arguments
     * @return the method execution result
     * @throws Exception if method invocation fails
     */
    private Object invokeControllerMethod(RouteDefinition route, Object[] methodArgs) throws Exception {
        return route.getMethodRef().invoke(route.getController(), methodArgs);
    }

    /**
     * Handles exceptions that occur during request processing
     *
     * @param exchange  the HTTP exchange
     * @param exception the exception that occurred
     * @throws IOException if there's an I/O error sending the error response
     */
    private void handleRequestException(HttpExchange exchange, Exception exception) throws IOException {
        Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
        executeExceptionHandlers(exchange, cause);

        if (cause instanceof BettingException bettingException) {
            ResponseWriter.write(exchange, bettingException.getHttpStatusCode(),
                    bettingException.getMessage());
        } else {
            ResponseWriter.write(exchange, HTTP_INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_MESSAGE);
        }
    }

    /**
     * Executes exception handlers
     *
     * @param exchange the HTTP exchange
     * @param cause    the exception cause
     */
    private void executeExceptionHandlers(HttpExchange exchange, Throwable cause) {
        interceptors.forEach(interceptor -> interceptor.afterException(exchange, cause));
    }

    /**
     * Sends a 404 Not Found response
     *
     * @param exchange the HTTP exchange
     * @throws IOException if there's an I/O error
     */
    private void sendNotFoundResponse(HttpExchange exchange) throws IOException {
        ResponseWriter.write(exchange, HTTP_NOT_FOUND, NOT_FOUND_MESSAGE);
    }

    /**
     * Resolves method parameters using the parameter resolver chain
     *
     * @param method   the controller method
     * @param exchange the HTTP exchange
     * @param context  the request context
     * @return array of resolved method arguments
     */
    private Object[] resolveMethodParameters(Method method, HttpExchange exchange, RequestContext context) {
        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            arguments[i] = resolvers.stream()
                    .filter(resolver -> resolver.supports(parameter))
                    .findFirst()
                    .map(resolver -> resolver.resolve(parameter, exchange,
                            context.pathParams(), context.queryParams(), context.body()))
                    .orElse(null);
        }

        return arguments;
    }
}



