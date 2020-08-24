package io.forestframework.ext.core;

import com.github.blindpirate.annotationmagic.Extends;
import io.forestframework.ext.api.EnableExtensions;
import io.forestframework.ext.api.Extension;
import io.forestframework.ext.api.StartupContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Extends(EnableExtensions.class)
@EnableExtensions(extensions = IncludeGraalJs.IncludeGraalJsComponentExtension.class)
public @interface IncludeGraalJs {
    String value();

    class IncludeGraalJsComponentExtension implements Extension {
        @Override
        public void beforeInjector(StartupContext startupContext) {
            String graalJsComponentName = startupContext.getEnableExtensionsAnnotation(IncludeGraalJs.class)
                    .stream()
                    .map(IncludeGraalJs::value)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Graaljs component not found"));
        }
    }
}
