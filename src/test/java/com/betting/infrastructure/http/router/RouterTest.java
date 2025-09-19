package com.betting.infrastructure.http.router;


import com.betting.api.controller.SessionController;
import com.betting.api.controller.StakeController;
import com.betting.core.model.Session;
import com.betting.core.service.SessionService;
import com.betting.core.service.StakeService;
import com.betting.infrastructure.http.resolver.BodyParamResolver;
import com.betting.infrastructure.http.resolver.HttpExchangeResolver;
import com.betting.infrastructure.http.resolver.PathParamResolver;
import com.betting.infrastructure.http.resolver.QueryParamResolver;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class RouterTest {
    private Router router;
    private SessionService mockSessionService;
    private StakeService mockStakeService;

    @BeforeEach
    void setUp() {
        mockSessionService = Mockito.mock(SessionService.class);
        mockStakeService = Mockito.mock(StakeService.class);

        router = new Router(List.of(
                new PathParamResolver(),
                new QueryParamResolver(),
                new BodyParamResolver(),
                new HttpExchangeResolver()
        ));
        router.registerControllers(
                new SessionController(mockSessionService),
                new StakeController(mockSessionService, mockStakeService)
        );
    }

    @Test
    void testGetSession() throws Exception {
        Session session = new Session(1234, "SESSION1234", System.currentTimeMillis() + 1000 * 60 * 60);
        when(mockSessionService.createOrGetSession(1234)).thenReturn(session);

        HttpExchange exchange = MockHttpExchange.create("GET", "/1234/session", null);

        router.route(exchange);

        String response = MockHttpExchange.getResponse(exchange);
        assertEquals("SESSION1234", response);
        assertEquals(200, exchange.getResponseCode());
    }

    @Test
    void testPostStake() throws Exception {
        when(mockSessionService.validateSession("SESSION1234")).thenReturn(true);
        when(mockSessionService.getCustomerId("SESSION1234")).thenReturn(5678);
        doNothing().when(mockStakeService).submitStake(5678, 1234, 1000);
        String body = "{\"stake\":1000}";
        HttpExchange exchange = MockHttpExchange.create("POST", "/1234/stake?sessionkey=SESSION1234", body);

        router.route(exchange);
        // 验证 service 被调用
        Mockito.verify(mockStakeService).submitStake(5678, 1234, 1000);

        String response = MockHttpExchange.getResponse(exchange);
        assertEquals("", response);
        assertEquals(200, exchange.getResponseCode());
    }

    @Test
    void testGetHighStakes() throws Exception {
        when(mockSessionService.validateSession("SESSION1234")).thenReturn(true);
        when(mockSessionService.getCustomerId("SESSION1234")).thenReturn(5678);
        List<String> highStakes = List.of("1234=1000", "5678=500"); // Using List.of() for immutable list
        when(mockStakeService.getTop20Stakes(5678)).thenReturn(highStakes);

        HttpExchange exchange = MockHttpExchange.create("GET", "/5678/highstakes", null);

        router.route(exchange);

        String response = MockHttpExchange.getResponse(exchange);
        assertEquals("1234=1000,5678=500", response);
        assertEquals(200, exchange.getResponseCode());
    }

    /**
     * 简单的 Mock HttpExchange 工具类
     */
    static class MockHttpExchange extends com.sun.net.httpserver.HttpExchange {
        private final String method;
        private final URI uri;
        private final String body;
        private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        private final Headers headers = new Headers();
        private int responseCode;

        static MockHttpExchange create(String method, String path, String body) {
            return new MockHttpExchange(method, path, body == null ? "" : body);
        }

        private MockHttpExchange(String method, String path, String body) {
            this.method = method;
            this.uri = URI.create("http://localhost" + path);
            this.body = body;
            headers.add("Content-Type", "application/json");
            if (body != null){
                headers.add("Content-Length", String.valueOf(body.length()));
            }
        }

        @Override
        public Headers getRequestHeaders() {
            return headers;
        }

        @Override
        public Headers getResponseHeaders() {
            return headers;
        }

        @Override
        public URI getRequestURI() {
            return uri;
        }

        @Override
        public String getRequestMethod() {
            return method;
        }

        @Override
        public HttpContext getHttpContext() {
            return null;
        }

        @Override
        public void close() {

        }

        @Override
        public ByteArrayInputStream getRequestBody() {
            return new ByteArrayInputStream(body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public ByteArrayOutputStream getResponseBody() {
            return responseBody;
        }

        @Override
        public void sendResponseHeaders(int rCode, long responseLength) {
            this.responseCode = rCode;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public int getResponseCode() {
            return responseCode;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public String getProtocol() {
            return "";
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }

        @Override
        public void setAttribute(String name, Object value) {

        }

        @Override
        public void setStreams(InputStream i, OutputStream o) {

        }

        @Override
        public HttpPrincipal getPrincipal() {
            return null;
        }

        static String getResponse(HttpExchange exchange) {
            return ((MockHttpExchange) exchange).responseBody.toString();
        }

        // other methods can be left unimplemented or return default values
    }
}
