package no.ssb.testing.helidon;

import io.grpc.BindableService;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.util.MutableHandlerRegistry;
import io.helidon.config.Config;
import io.helidon.config.spi.ConfigSource;
import io.helidon.grpc.server.GrpcServer;
import io.helidon.webserver.WebServer;
import no.ssb.helidon.application.HelidonApplication;
import no.ssb.helidon.application.HelidonApplicationBuilder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static io.helidon.config.ConfigSources.classpath;
import static io.helidon.config.ConfigSources.file;

public class IntegrationTestExtension implements BeforeEachCallback, BeforeAllCallback, AfterAllCallback {

    TestClient client;
    HelidonApplication application;
    ManagedChannel grpcChannel;
    Server server;

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        ServiceLoader<HelidonApplicationBuilder> applicationBuilderLoader = ServiceLoader.load(HelidonApplicationBuilder.class);
        HelidonApplicationBuilder applicationBuilder = applicationBuilderLoader.findFirst().orElseThrow();

        List<Supplier<ConfigSource>> configSourceSupplierList = new LinkedList<>();
        String overrideFile = System.getenv("HELIDON_CONFIG_FILE");
        if (overrideFile != null) {
            configSourceSupplierList.add(file(overrideFile).optional());
        }
        String profile = System.getenv("HELIDON_CONFIG_PROFILE");
        if (profile == null) {
            profile = "dev";
        }
        if (profile.equalsIgnoreCase("dev")) {
            configSourceSupplierList.add(classpath("application-dev.yaml"));
        } else if (profile.equalsIgnoreCase("drone")) {
            configSourceSupplierList.add(classpath("application-drone.yaml"));
        } else {
            // default to dev
            configSourceSupplierList.add(classpath("application-dev.yaml"));
        }
        configSourceSupplierList.add(classpath("application.yaml"));
        Config config = Config.builder().sources(configSourceSupplierList).build();
        applicationBuilder.override(Config.class, config);

        Class<?> testClass = extensionContext.getRequiredTestClass();
        GrpcMockRegistryConfig applicationConfig = testClass.getDeclaredAnnotation(GrpcMockRegistryConfig.class);
        if (applicationConfig != null) {
            Class<? extends GrpcMockRegistry> registryClazz = applicationConfig.value();
            Constructor<?> constructor = registryClazz.getDeclaredConstructors()[0];
            GrpcMockRegistry grpcMockRegistry = (GrpcMockRegistry) constructor.newInstance();

            String serverName = InProcessServerBuilder.generateName();

            MutableHandlerRegistry serviceRegistry = new MutableHandlerRegistry();

            for (BindableService bindableService : grpcMockRegistry) {
                serviceRegistry.addService(bindableService);
            }

            server = InProcessServerBuilder.forName(serverName).fallbackHandlerRegistry(serviceRegistry).directExecutor().build().start();
            ManagedChannel channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();

            applicationBuilder.override(ManagedChannel.class, channel);
        }

        application = applicationBuilder.build();

        application.start().toCompletableFuture().get(5, TimeUnit.SECONDS);

        grpcChannel = ManagedChannelBuilder.forAddress("localhost", application.get(GrpcServer.class).port())
                .usePlaintext()
                .build();

        client = TestClient.newClient("localhost", application.get(WebServer.class).port());
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
            if (Channel.class.isAssignableFrom(field.getType()) || ManagedChannel.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    if (field.get(test) == null) {
                        field.set(test, grpcChannel);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        if (server != null) {
            server.shutdown();
        }
        application.stop();
        shutdownAndAwaitTermination(grpcChannel);
        if (server != null) {
            awaitTerminationOfGrpcServer(server);
        }
    }

    void shutdownAndAwaitTermination(ManagedChannel managedChannel) {
        managedChannel.shutdown();
        try {
            if (!managedChannel.awaitTermination(5, TimeUnit.SECONDS)) {
                managedChannel.shutdownNow(); // Cancel currently executing tasks
                if (!managedChannel.awaitTermination(5, TimeUnit.SECONDS))
                    System.err.println("ManagedChannel did not terminate");
            }
        } catch (InterruptedException ie) {
            managedChannel.shutdownNow(); // (Re-)Cancel if current thread also interrupted
            Thread.currentThread().interrupt();
        }
    }

    void awaitTerminationOfGrpcServer(Server server) {
        try {
            if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                server.shutdownNow(); // Cancel currently executing tasks
                if (!server.awaitTermination(5, TimeUnit.SECONDS))
                    System.err.println("Server did not terminate");
            }
        } catch (InterruptedException e) {
            server.shutdownNow(); // (Re-)Cancel if current thread also interrupted
            Thread.currentThread().interrupt();
        }
    }
}
