package io.forestframework.core.http.routing;

import com.github.blindpirate.annotationmagic.AnnotationMagic;
import io.forestframework.core.Forest;
import io.forestframework.core.config.ConfigProvider;
import io.forestframework.core.http.Router;
import io.forestframework.ext.api.DefaultStartupContext;
import io.forestframework.ext.api.EnableExtensions;
import io.forestframework.ext.api.Extension;
import io.forestframework.ext.api.StartupContext;
import io.forestframework.ext.core.AutoRoutingScanExtension;
import io.forestframework.ext.core.IncludeComponents;
import io.forestframework.utils.StartupUtils;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@EnableExtensions(extensions = AutoRoutingScanExtension.class)
@IncludeComponents(classes = ThrowExceptionsWhenPreHandlerReturnTypeIsNotValid.class)
class StartupFailureApp {
}

@Router("/preHandlerNotReturnBoolean")
class ThrowExceptionsWhenPreHandlerReturnTypeIsNotValid extends AbstractTraceableRouter {

    @Route(value = "/**", type = RoutingType.PRE_HANDLER)
    public String preHandler(HttpServerRequest request) {
        addToTrace("prehandlerReturnsString");
        return "shouldNotBeHere";
    }

    @Route(value = "/**", type = RoutingType.HANDLER)
    public void handler(HttpServerRequest request, HttpServerResponse response) {
        addToTrace(Message.HANDLER.name());
    }

    @OnError(value = "/**")
    public void customErrorHandler(HttpServerResponse response, Throwable e) {
        response.write(" should be handled by custom error handler. ");
        response.write(" e.getMessage(): " + e.getMessage());
        addToTrace(Message.CUSTOM_ERROR_HANDLER.name());
    }

    @PostHandler("/**")
    public void postHandler(HttpServerResponse response) {
        addToTrace("shouldContinueInPostHandler");
    }
}

public class StartupFailureTest {
    @Test
    public void shouldThrowExceptionAtStartTimeWhenPreHandlerReturnTypeIsNotValid() {
        Class<?> appClass = StartupFailureApp.class;
        List<Extension> extensions = AnnotationMagic.getAnnotationsOnClass(appClass, EnableExtensions.class)
                .stream().flatMap(it -> Stream.of(it.extensions()))
                .map(StartupUtils::instantiateWithDefaultConstructor)
                .map(it -> (Extension) it)
                .collect(Collectors.toList());

        StartupContext context = new DefaultStartupContext(Vertx.vertx(), appClass, ConfigProvider.load(), extensions);

        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> Forest.run(context));
        Assertions.assertEquals("PreHandler return type is not valid!", e.getMessage());
    }
}
