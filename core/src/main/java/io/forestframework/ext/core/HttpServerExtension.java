package io.forestframework.ext.core;

import com.google.inject.Injector;
import io.forestframework.core.config.ConfigProvider;
import io.forestframework.core.http.DefaultHttpVerticle;
import io.forestframework.core.http.routing.DefaultRoutingManager;
import io.forestframework.core.http.routing.RoutingManager;
import io.forestframework.ext.api.Extension;
import io.forestframework.utils.completablefuture.VertxCompletableFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.apiguardian.api.API;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;


/**
 * Manage HTTP-server-related work at startup:
 *
 * <ol>
 *     <li>1. Scan and register all {@link io.forestframework.core.http.routing.Routing}s from component classes.</li>
 *     <li>2. Start the HTTP server. </li>
 *     <li>3. Block until the HTTP server starts. </li>
 * </ol>
 */
@API(status = API.Status.INTERNAL, since = "0.1")
public class HttpServerExtension implements Extension {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerExtension.class);
    private static final Pattern BRIDGE_ROUTING_PATH_PATTERN = Pattern.compile("(/|[^*:])*/?");

    private String deploymentId;
    private Vertx vertx;

    @Override
    public void afterInjector(Injector injector) {
        vertx = injector.getInstance(Vertx.class);

        startHttpServer(injector);

    }

    @Override
    public void close() throws Exception {
        VertxCompletableFuture.from(vertx.getOrCreateContext(), vertx.undeploy(deploymentId)).get();
    }

    protected void startHttpServer(Injector injector) {
        ConfigProvider configProvider = injector.getInstance(ConfigProvider.class);
        DeploymentOptions deploymentOptions = configProvider.getInstance("forest.deploy", DeploymentOptions.class);

        ((DefaultRoutingManager) injector.getInstance(RoutingManager.class)).finalizeRoutings();

        Future<String> vertxFuture = vertx.deployVerticle(() -> injector.getInstance(DefaultHttpVerticle.class), deploymentOptions);
        CompletableFuture<String> future = VertxCompletableFuture.from(vertx.getOrCreateContext(), vertxFuture);
        try {
            deploymentId = future.get();
            LOGGER.info("Http server successful started on {} with {} instances", configProvider.getInstance("forest.http.port", String.class), deploymentOptions.getInstances());
        } catch (Throwable e) {
            LOGGER.error("", e);
            throw new RuntimeException(e);
        }
    }
}
