package com.betting;

import com.betting.api.controller.SessionController;
import com.betting.api.controller.StakeController;
import com.betting.core.service.SessionService;
import com.betting.core.service.StakeService;
import com.betting.core.service.impl.SessionServiceImpl;
import com.betting.core.service.impl.StakeServiceImpl;
import com.betting.infrastructure.config.BettingConfig;
import com.betting.infrastructure.http.interceptor.LoggingInterceptor;
import com.betting.infrastructure.http.resolver.*;
import com.betting.infrastructure.http.router.Router;
import com.betting.util.Logger;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Main Application Class for Betting Service
 * <p>
 * This is the main entry point for the betting service application.
 * It provides improved resource management, monitoring capabilities, and graceful shutdown.
 * <p>
 * Key Features:
 * - Fixed thread pool configuration for optimal performance
 * - Graceful shutdown with resource cleanup
 * <p>
 * Configuration:
 * - Port: 8001 (configurable)
 * - Thread Pool Size: 50 threads (configurable)
 * - Shutdown Timeout: 5 seconds
 */
public class BettingApplication {

    private static final BettingConfig config = new BettingConfig();
    private HttpServer server;
    private SessionService sessionService;

    public static void main(String[] args) throws Exception {
        BettingApplication app = new BettingApplication();
        app.start();

        // Add graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));

        // Keep application running
        Thread.currentThread().join();
    }

    public void start() throws Exception {
        Logger.info("Starting betting service...");

        // Initialize services
        sessionService = new SessionServiceImpl();
        StakeService stakeService = new StakeServiceImpl();

        // Create router
        Router router = createRouter(sessionService, stakeService);

        // Start HTTP server
        server = HttpServer.create(new InetSocketAddress(config.getPort()), 0);
        server.createContext("/", router::route);
        server.setExecutor(Executors.newFixedThreadPool(config.getThreadPoolSize()));
        server.start();

        Logger.info("Server started on port %d with %d threads", config.getPort(), config.getThreadPoolSize());
    }

    private Router createRouter(SessionService sessionService, StakeService stakeService) {
        // Create parameter resolvers
        List<ParamResolver> resolvers = List.of(
                new PathParamResolver(),
                new QueryParamResolver(),
                new BodyParamResolver(),
                new HttpExchangeResolver()
        );

        // Create router
        Router router = new Router(resolvers);

        // Add interceptors
        router.addInterceptors(new LoggingInterceptor());

        // Register controllers
        router.registerControllers(
                new SessionController(sessionService),
                new StakeController(sessionService, stakeService)
        );

        return router;
    }


    public void shutdown() {
        Logger.info("Shutting down betting service...");

        try {
            // Stop HTTP server
            if (server != null) {
                server.stop(5); // 5 second timeout
            }

            // Shutdown session service
            if (sessionService instanceof SessionServiceImpl sessionServiceImpl) {
                sessionServiceImpl.shutdown();
            }

            Logger.info("Service shutdown completed");
        } catch (Exception e) {
            Logger.error("Error during shutdown", e, e.getMessage());
        }
    }
}



