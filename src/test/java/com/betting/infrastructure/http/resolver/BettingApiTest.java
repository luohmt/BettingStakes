package com.betting.infrastructure.http.resolver;

import com.betting.api.controller.SessionController;
import com.betting.api.controller.StakeController;
import com.betting.core.model.Session;
import com.betting.core.service.SessionService;
import com.betting.core.service.StakeService;
import com.betting.infrastructure.http.interceptor.LoggingInterceptor;
import com.betting.infrastructure.http.router.Router;
import com.sun.net.httpserver.HttpServer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BettingApiTest {

    private HttpServer server;
    private SessionService mockSessionService;
    private StakeService mockStakeService;

    @BeforeAll
    void startServer() throws Exception {
        // 创建 Mock 服务
        mockSessionService = Mockito.mock(SessionService.class);
        mockStakeService = Mockito.mock(StakeService.class);

        // Mock 行为
        Session session = new Session(1234, "SESSION1234", System.currentTimeMillis() + 3600000);
        when(mockSessionService.createOrGetSession(1234)).thenReturn(session);
        when(mockSessionService.validateSession("SESSION1234")).thenReturn(true);
        when(mockSessionService.getCustomerId("SESSION1234")).thenReturn(1234);
        List<String> top20Stakes = List.of("1234=1000,5678=500");
        when(mockStakeService.getTop20Stakes(5678))
                .thenReturn(top20Stakes);

        // 创建 Router
        Router router = new Router(List.of(
                new PathParamResolver(),
                new QueryParamResolver(),
                new BodyParamResolver(),
                new HttpExchangeResolver()
        ));

        router.addInterceptors(new LoggingInterceptor());
        router.registerControllers(
                new SessionController(mockSessionService),
                new StakeController(mockSessionService, mockStakeService)
        );

        // 启动 HttpServer
        server = HttpServer.create(new InetSocketAddress(8082), 0);
        server.createContext("/", router::route);
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        // 配置 RestAssured
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8082;
    }

    @AfterAll
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testGetSession() {
        given()
                .get("/1234/session")
                .then()
                .statusCode(200)
                .body(equalTo("SESSION1234"));
    }

    @Test
    void testGetHighStakes() {
        given()
                .get("/5678/highstakes")
                .then()
                .statusCode(200)
                .body(equalTo("1234=1000,5678=500"));
    }
}


