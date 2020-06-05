package org.forestframework.annotationmagic;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an annotation extends (inherits) another annotation.
 * Similar to class inheritance, if annotation X extends annotation Y,
 * when searching Y annotation, X annotation will also be returned.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Extends {
    Class<? extends Annotation> value();
}
