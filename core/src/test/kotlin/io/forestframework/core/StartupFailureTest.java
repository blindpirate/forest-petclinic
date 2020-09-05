package io.forestframework.core;

import com.github.blindpirate.annotationmagic.AnnotationMagic;
import io.forestframework.core.config.ConfigProvider;
import io.forestframework.ext.api.DefaultStartupContext;
import io.forestframework.ext.api.EnableExtensions;
import io.forestframework.ext.api.Extension;
import io.forestframework.ext.api.StartupContext;
import io.forestframework.ext.core.IncludeComponents;
import io.forestframework.utils.StartupUtils;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class StartupFailureTest {
    @Test
    public void test() {
        Class<?> appClaass = App.class;
        List<Extension> extensions = AnnotationMagic.getAnnotationsOnClass(appClaass, EnableExtensions.class)
                .stream().flatMap(it -> Stream.of(it.extensions()))
                .map(StartupUtils::instantiateWithDefaultConstructor)
                .map(it -> (Extension) it)
                .collect(Collectors.toList());

        StartupContext context = new DefaultStartupContext(Vertx.vertx(), appClaass, ConfigProvider.load(), extensions);

        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> Forest.run(context));
        Assertions.assertEquals("Unlucky!", e.getMessage());
    }

    public static class MyComponent {
    }

    public static class ErrorExtension implements Extension {
        @Override
        public void beforeInjector(StartupContext startupContext) {
            throw new RuntimeException("Unlucky!");
        }
    }

    @IncludeComponents(classes = MyComponent.class)
    @EnableExtensions(extensions = ErrorExtension.class)
    public static class App {
    }
}
