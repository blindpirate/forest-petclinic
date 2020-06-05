package org.forestframework.extensions;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;
import org.forestframework.annotation.Route;
import org.forestframework.ext.api.ComponentClassConfigurer;
import org.forestframework.http.HttpMethod;

import javax.inject.Singleton;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A thin-layer encapsulation of {@link CorsHandler} for
 * http://www.w3.org/TR/cors/[CORS] support.
 */
public class CorsExtension implements ComponentClassConfigurer {
    @Override
    public void configure(LinkedHashSet<Class<?>> componentClasses) {
        componentClasses.add(CorsRouter.class);
    }
}

@Singleton
class CorsRouter {
    private Set<String> allowedHeaders = new LinkedHashSet<>();
    private CorsHandler corsHandler = CorsHandler.create("*");

    public CorsRouter() {
        allowedHeaders.add("x-requested-with");
        allowedHeaders.add("Access-Control-Allow-Origin");
        allowedHeaders.add("origin");
        allowedHeaders.add("Content-Type");
        allowedHeaders.add("accept");
        corsHandler.allowedHeaders(allowedHeaders);
        Stream.of(HttpMethod.values()).forEach(method -> corsHandler.allowedMethod(method.toVertxHttpMethod()));
    }

    @Route("/*")
    public void enableCors(RoutingContext routingContext) {
        corsHandler.handle(routingContext);
    }
}


