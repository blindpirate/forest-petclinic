package io.forestframework.core.http.param;

import com.github.blindpirate.annotationmagic.AnnotationMagic;
import io.forestframework.core.config.Converter;
import io.forestframework.core.http.HttpContext;
import io.forestframework.core.http.routing.Routing;

import javax.inject.Singleton;

@Singleton
public class QueryParamResolver implements RoutingParameterResolver<HttpContext> {
    @SuppressWarnings("unchecked")
    @Override
    public Object resolveParameter(HttpContext context, Routing routing, int paramIndex) {
        QueryParam anno = AnnotationMagic.getOneAnnotationOnMethodParameterOrNull(routing.getHandlerMethod(), paramIndex, QueryParam.class);
        Class<?> paramType = routing.getHandlerMethod().getParameterTypes()[paramIndex];

        String param = context.request().getParam(anno.value());
        if (param == null) {
            if (anno.optional()) {
                return Converter.getDefaultConverter().convert(anno.defaultValue(), String.class, paramType);
            } else {
                throw new IllegalArgumentException("Param " + paramIndex + " of " + routing.getHandlerMethod() + " not found!");
            }
        } else {
            return Converter.getDefaultConverter().convert(param, String.class, paramType);
        }
    }
}
