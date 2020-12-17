package io.forestframework.ext.core;

import com.google.common.reflect.ClassPath;
import io.forestframework.ext.api.ApplicationContext;
import io.forestframework.ext.api.Extension;
import io.forestframework.utils.StartupUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import static com.google.common.reflect.ClassPath.from;
import static io.forestframework.ext.core.AutoScanComponents.APPLICATION_PACKAGE;

public class AutoComponentScanExtension implements Extension {
    private final List<String> basePackages;

    private AutoComponentScanExtension(List<String> basePackages) {
        this.basePackages = basePackages;
    }

    public AutoComponentScanExtension() {
        this(Arrays.asList(APPLICATION_PACKAGE));
    }

    public AutoComponentScanExtension(AutoScanComponents autoScanComponents) {
        this(Arrays.asList(autoScanComponents.basePackages()));
    }

    @Override
    public void start(ApplicationContext applicationContext) {
        LinkedHashSet<Class<?>> componentClasses = new LinkedHashSet<>(applicationContext.getComponents());
        basePackages.forEach(packageName -> scanAndAddComponentClasses(applicationContext.getAppClass(), packageName, componentClasses));
        applicationContext.getComponents().clear();
        applicationContext.getComponents().addAll(componentClasses);
    }

    @SuppressWarnings("UnstableApiUsage")
    private void scanAndAddComponentClasses(Class<?> appClass, String packageName, LinkedHashSet<Class<?>> resultSet) {
        if (APPLICATION_PACKAGE.equals(packageName)) {
            packageName = appClass.getPackage().getName();
        }
        try {
            from(getClass().getClassLoader())
                .getTopLevelClassesRecursive(packageName)
                .stream()
                .map(ClassPath.ClassInfo::load)
                .filter(StartupUtils::isComponentClass)
                .forEach(resultSet::add);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
