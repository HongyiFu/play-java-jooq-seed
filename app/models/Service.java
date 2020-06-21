package models;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Just a marker interface.
 */
@Target(TYPE)
@Retention(SOURCE)
public @interface Service {
}
