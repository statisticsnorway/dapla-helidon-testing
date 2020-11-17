package no.ssb.testing.helidon;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.webserver.WebServer;
import no.ssb.helidon.application.HelidonApplication;
import no.ssb.helidon.application.HelidonApplicationBuilder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

import static io.helidon.config.ConfigSources.classpath;
import static io.helidon.config.ConfigSources.file;
import static java.util.Optional.ofNullable;

public class IntegrationTestExtension implements BeforeEachCallback, BeforeAllCallback, AfterAllCallback {

    TestClient client;
    HelidonApplication application;

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        Class<?> testClass = extensionContext.getRequiredTestClass();

        Config.Builder builder = Config.builder();
        ConfigOverride configOverride = testClass.getDeclaredAnnotation(ConfigOverride.class);
        if (configOverride != null) {
            String[] overrideArray = configOverride.value();
            Map<String, String> configOverrideMap = new LinkedHashMap<>();
            for (int i = 0; i < overrideArray.length; i += 2) {
                configOverrideMap.put(overrideArray[i], overrideArray[i + 1]);
            }
            builder.addSource(ConfigSources.create(configOverrideMap));
        }
        String overrideFile = ofNullable(System.getProperty("helidon.config.file")).orElseGet(() -> System.getenv("HELIDON_CONFIG_FILE"));
        if (overrideFile != null) {
            builder.addSource(file(overrideFile).optional());
        }
        String profile = ofNullable(System.getProperty("helidon.config.profile")).orElseGet(() -> System.getenv("HELIDON_CONFIG_PROFILE"));
        if (profile == null) {
            profile = "dev";
        }
        if (profile.equalsIgnoreCase("dev")) {
            builder.addSource(classpath("application-dev.yaml"));
        } else if (profile.equalsIgnoreCase("azure")) {
            builder.addSource(classpath("application-azure.yaml"));
        } else {
            // default to dev
            builder.addSource(classpath("application-dev.yaml"));
        }
        builder.addSource(classpath("application.yaml"));
        Config config = builder.build();

        ServiceLoader<HelidonApplicationBuilder> applicationBuilderLoader = ServiceLoader.load(HelidonApplicationBuilder.class);
        HelidonApplicationBuilder applicationBuilder = applicationBuilderLoader.findFirst().orElseThrow();
        applicationBuilder.override(Config.class, config);

        application = applicationBuilder.build();

        application.start().toCompletableFuture().get(5, TimeUnit.SECONDS);

        WebServer webServer = application.get(WebServer.class);
        if (webServer != null) {
            client = TestClient.newClient("localhost", webServer.port());
        }
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        Object test = extensionContext.getRequiredTestInstance();
        Field[] fields = test.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!field.isAnnotationPresent(Inject.class)) {
                continue;
            }
            // application
            if (HelidonApplication.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    if (field.get(test) == null) {
                        field.set(test, application);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            if (TestClient.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    if (field.get(test) == null) {
                        field.set(test, client);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        if (application != null) {
            application.stop();
        }
    }
}
