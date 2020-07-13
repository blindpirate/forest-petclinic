package io.forestframework.core.http;

import io.forestframework.core.config.Config;
import io.forestframework.core.http.routing.RequestHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class DefaultHttpVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHttpVerticle.class);
    private final Vertx vertx;
    private final RequestHandler routingEngine;
    private final HttpServerOptions httpServerOptions;

    @Inject
    public DefaultHttpVerticle(Vertx vertx,
                               RequestHandler routingEngine,
                               @Config("forest.http") HttpServerOptions httpServerOptions
    ) {
        this.vertx = vertx;
        this.routingEngine = routingEngine;
        this.httpServerOptions = httpServerOptions;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        try {
            vertx.createHttpServer(httpServerOptions)
                    .requestHandler(routingEngine)
                    .exceptionHandler(e -> LOGGER.error("", e))
                    .listen(result -> {
                        if (result.succeeded()) {
                            LOGGER.info("Success");
                            startPromise.complete();
                        } else {
                            LOGGER.error("", result.cause());
                            result.cause().printStackTrace();
                            startPromise.fail(result.cause());
                        }
                    });
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}