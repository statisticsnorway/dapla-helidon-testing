package no.ssb.testing.helidon;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MockRegistryConfig {
    /**
     * A {@link MockRegistry} class to instanciate and use as to register.
     */
    Class<? extends MockRegistry> value();
}