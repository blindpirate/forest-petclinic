package io.forestframework.ext.core;

import com.github.blindpirate.annotationmagic.Extends;
import io.forestframework.ext.api.ApplicationContext;
import io.forestframework.ext.api.Extension;
import io.forestframework.ext.api.WithExtensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.stream.Stream;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Extends(WithExtensions.class)
@WithExtensions(extensions = IncludeComponents.IncludeComponentExtension.class)
public @interface IncludeComponents {
    Class<?>[] classes() default {};

    class IncludeComponentExtension implements Extension {
        private final IncludeComponents includeComponents;

        public IncludeComponentExtension(IncludeComponents includeComponents) {
            this.includeComponents = includeComponents;
        }

        @Override
        public void start(ApplicationContext applicationContext) {
            Stream.of(includeComponents.classes()).forEach(applicationContext.getComponents()::add);
        }
    }
}
