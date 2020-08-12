package io.forestframework.core.http.websocket;

import com.github.blindpirate.annotationmagic.Extends;
import io.forestframework.core.http.routing.Route;
import io.forestframework.core.http.routing.RoutingType;
import io.forestframework.core.http.sockjs.SockJS;
import io.forestframework.core.http.sockjs.SockJSEventType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Extends(Route.class)
@Route(type = RoutingType.ON_WEB_SOCKET_ERROR)
public @interface OnWSError {
    String value() default "";
}
