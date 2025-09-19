package com.betting.infrastructure.http.resolver;

import com.betting.infrastructure.http.router.BodyParam;
import com.betting.infrastructure.http.router.PathParam;
import com.betting.infrastructure.http.router.QueryParam;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * validate all ParamResolver implementations
 */
class ResolverTest {

    @Mock
    private HttpExchange mockExchange;

    private Map<String, String> pathParams;
    private Map<String, String> queryParams;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // init pathParams and queryParams
        pathParams = new HashMap<>();
        queryParams = new HashMap<>();
    }

    @Test
    void testBodyParamResolver() throws Exception {
        BodyParamResolver resolver = new BodyParamResolver();

        //public void testMethod(@BodyParam("name") String name, @BodyParam String body)
        Method testMethod = TestClass.class.getMethod("testMethod", String.class, String.class);
        Parameter[] parameters = testMethod.getParameters();

        // @BodyParam("name") String name
        Parameter nameParam = parameters[0];
        assertTrue(resolver.supports(nameParam));

        String jsonBody = "{\"name\":\"张三\",\"age\":25}";
        Object result = resolver.resolve(nameParam, mockExchange, pathParams, queryParams, jsonBody);
        assertEquals("张三", result);

        //@BodyParam String body
        Parameter bodyParam = parameters[1];
        assertTrue(resolver.supports(bodyParam));

        Object bodyResult = resolver.resolve(bodyParam, mockExchange, pathParams, queryParams, jsonBody);
        assertEquals(jsonBody, bodyResult);

        // test null body
        Object nullResult = resolver.resolve(nameParam, mockExchange, pathParams, queryParams, null);
        assertNull(nullResult);

        // test missing key
        Object missingKeyResult = resolver.resolve(nameParam, mockExchange, pathParams, queryParams, "{\"age\":25}");
        assertEquals("", missingKeyResult);
    }

    @Test
    void testPathParamResolver() throws Exception {
        PathParamResolver resolver = new PathParamResolver();

        // public void testMethod(@PathParam("id") Long id)
        Method testMethod = TestClass.class.getMethod("testPathMethod", Long.class);
        Parameter parameter = testMethod.getParameters()[0];

        assertTrue(resolver.supports(parameter));

        pathParams.put("id", "123");
        Object result = resolver.resolve(parameter, mockExchange, pathParams, queryParams, null);
        assertEquals(123L, result);

        // test null
        pathParams.clear();
        Object nullResult = resolver.resolve(parameter, mockExchange, pathParams, queryParams, null);
        assertNull(nullResult);

        // test invalid number
        pathParams.put("id", "invalid");
        Object invalidResult = resolver.resolve(parameter, mockExchange, pathParams, queryParams, null);
        assertNull(invalidResult);
    }

    @Test
    void testQueryParamResolver() throws Exception {
        QueryParamResolver resolver = new QueryParamResolver();

        // public void testMethod(@QueryParam("page") Integer page)
        Method testMethod = TestClass.class.getMethod("testQueryMethod", Integer.class);
        Parameter parameter = testMethod.getParameters()[0];
        assertTrue(resolver.supports(parameter));

        queryParams.put("page", "5");
        Object result = resolver.resolve(parameter, mockExchange, pathParams, queryParams, null);
        assertEquals(5, result);

        // test null
        queryParams.clear();
        Object nullResult = resolver.resolve(parameter, mockExchange, pathParams, queryParams, null);
        assertNull(nullResult);

        // test boolean
        Method boolMethod = TestClass.class.getMethod("testBoolMethod", Boolean.class);
        Parameter boolParam = boolMethod.getParameters()[0];

        queryParams.put("enabled", "true");
        Object boolResult = resolver.resolve(boolParam, mockExchange, pathParams, queryParams, null);
        assertEquals(true, boolResult);
    }

    @Test
    void testHttpExchangeResolver() throws Exception {
        HttpExchangeResolver resolver = new HttpExchangeResolver();

        // public void testMethod(HttpExchange exchange)
        Method testMethod = TestClass.class.getMethod("testHttpMethod", HttpExchange.class);
        Parameter parameter = testMethod.getParameters()[0];

        assertTrue(resolver.supports(parameter));

        Object result = resolver.resolve(parameter, mockExchange, pathParams, queryParams, null);
        assertEquals(mockExchange, result);

        // test unsupported type
        Method stringMethod = TestClass.class.getMethod("testStringMethod", String.class);
        Parameter stringParam = stringMethod.getParameters()[0];

        assertFalse(resolver.supports(stringParam));
    }

    @Test
    void testHttpExchangeResolverSendMethod() throws Exception {
        HttpExchangeResolver resolver = new HttpExchangeResolver();

        Method testMethod = TestClass.class.getMethod("testHttpMethod", HttpExchange.class);
        Parameter parameter = testMethod.getParameters()[0];

        assertTrue(resolver.supports(parameter));

        Object result = resolver.resolve(parameter, mockExchange, pathParams, queryParams, null);
        assertEquals(mockExchange, result);
    }

    @Test
    void testResolverSupportsMethod() throws Exception {
        // BodyParamResolver
        BodyParamResolver bodyResolver = new BodyParamResolver();
        Method testMethod = TestClass.class.getMethod("testMethod", String.class, String.class);

        assertTrue(bodyResolver.supports(testMethod.getParameters()[0])); // @BodyParam("name")
        assertTrue(bodyResolver.supports(testMethod.getParameters()[1])); // @BodyParam
        // PathParamResolver
        PathParamResolver pathResolver = new PathParamResolver();
        Method pathMethod = TestClass.class.getMethod("testPathMethod", Long.class);

        assertTrue(pathResolver.supports(pathMethod.getParameters()[0])); // @PathParam("id")
        assertFalse(pathResolver.supports(testMethod.getParameters()[0])); // @BodyParam注解

        // QueryParamResolver
        QueryParamResolver queryResolver = new QueryParamResolver();
        Method queryMethod = TestClass.class.getMethod("testQueryMethod", Integer.class);

        assertTrue(queryResolver.supports(queryMethod.getParameters()[0])); // @QueryParam("page")
        assertFalse(queryResolver.supports(testMethod.getParameters()[0])); // @BodyParam注解

        // HttpExchangeResolver
        HttpExchangeResolver httpResolver = new HttpExchangeResolver();
        Method httpMethod = TestClass.class.getMethod("testHttpMethod", HttpExchange.class);

        assertTrue(httpResolver.supports(httpMethod.getParameters()[0])); // HttpExchange类型
        assertFalse(httpResolver.supports(testMethod.getParameters()[0])); // String类型
    }

    /**
     * 测试用的内部类，包含各种参数注解的方法
     */
    public static class TestClass {
        public void testMethod(@BodyParam("name") String name, @BodyParam String body) {
            // do  nothing
        }

        public void testPathMethod(@PathParam("id") Long id) {
            // do  nothing
        }

        public void testQueryMethod(@QueryParam("page") Integer page) {
            // do  nothing
        }

        public void testBoolMethod(@QueryParam("enabled") Boolean enabled) {
            // do  nothing
        }

        public void testHttpMethod(HttpExchange exchange) {
            // do  nothing
        }

        public void testStringMethod(String str) {
            // do  nothing
        }
    }
}
