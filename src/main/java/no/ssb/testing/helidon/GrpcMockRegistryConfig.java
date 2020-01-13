package no.ssb.testing.helidon;

import org.junit.jupiter.api.extension.Extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcMockRegistryConfig {
    /**
     * An array of one or more {@link Extension} classes to register.
     */
    Class<? extends GrpcMockRegistry> value();

}
