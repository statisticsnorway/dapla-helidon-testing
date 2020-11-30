package no.ssb.testing.helidon;

import no.ssb.helidon.media.protobuf.ProtobufJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public final class TestClient {

    private static final Logger LOG = LoggerFactory.getLogger(TestClient.class);

    private final String host;
    private final int port;
    private final HttpClient client;

    private TestClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.client = HttpClient
                .newBuilder()
                .build();
    }

    public static TestClient newClient(String host, int port) {
        return new TestClient(host, port);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    URI toUri(String path) {
        try {
            return new URL(String.format("http://%s:%d%s", host, port, path)).toURI();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    static String captureStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    public ResponseHelper<String> options(String uri, String... headersKeyAndValue) {
        return options(uri, HttpResponse.BodyHandlers.ofString(), headersKeyAndValue);
    }

    public <R> ResponseHelper<R> options(String uri, HttpResponse.BodyHandler<R> bodyHandler, String... headersKeyAndValue) {
        try {
            HttpRequest request = HttpRequest.newBuilder(toUri(uri))
                    .headers(headersKeyAndValue)
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public ResponseHelper<String> head(String uri) {
        return head(uri, HttpResponse.BodyHandlers.ofString());
    }

    public <R> ResponseHelper<R> head(String uri, HttpResponse.BodyHandler<R> bodyHandler) {
        try {
            HttpRequest request = HttpRequest.newBuilder(toUri(uri))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public ResponseHelper<String> put(String uri, String... headers) {
        return put(uri, HttpRequest.BodyPublishers.noBody(), HttpResponse.BodyHandlers.ofString(), headers);
    }

    public <T> ResponseHelper<String> put(String uri, T pojo, String... headers) {
        return put(uri, HttpRequest.BodyPublishers.ofString(ProtobufJsonUtils.toString(pojo), StandardCharsets.UTF_8), HttpResponse.BodyHandlers.ofString(), headers);
    }

    public ResponseHelper<String> put(String uri, String body, String... headers) {
        return put(uri, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8), HttpResponse.BodyHandlers.ofString(), headers);
    }

    public <R> ResponseHelper<R> put(String uri, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<R> bodyHandler, String... headers) {
        try {
            final HttpRequest.Builder builder = HttpRequest.newBuilder(toUri(uri)).PUT(bodyPublisher);
            if (headers != null && headers.length > 0) {
                builder.headers(headers);
            }
            return new ResponseHelper<>(client.send(builder.build(), bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public ResponseHelper<String> post(String uri, String... headers) {
        return post(uri, HttpRequest.BodyPublishers.noBody(), HttpResponse.BodyHandlers.ofString(), headers);
    }

    public <R> ResponseHelper<R> post(String uri, Class<R> responseClazz, String... headers) {
        return post(
                uri,
                HttpRequest.BodyPublishers.noBody(),
                responseInfo -> new ProtobufSubscriber(responseClazz, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8).apply(responseInfo)),
                headers
        );
    }

    public <T> ResponseHelper<String> postAsJson(String uri, T pojo, String... headers) {
        String[] headersWithContentType = new String[headers.length + 2];
        for (int i = 0; i < headers.length; i++) {
            headersWithContentType[i] = headers[i];
        }
        headersWithContentType[headersWithContentType.length - 2] = "Content-Type";
        headersWithContentType[headersWithContentType.length - 1] = "application/json; charset=utf-8";
        return post(uri, HttpRequest.BodyPublishers.ofString(ProtobufJsonUtils.toString(pojo), StandardCharsets.UTF_8), HttpResponse.BodyHandlers.ofString(), headersWithContentType);
    }

    public <R, T> ResponseHelper<R> postAsJson(String uri, T pojo, Class<R> responseClazz, String... headers) {
        String[] headersWithContentType = new String[headers.length + 2];
        for (int i = 0; i < headers.length; i++) {
            headersWithContentType[i] = headers[i];
        }
        headersWithContentType[headersWithContentType.length - 2] = "Content-Type";
        headersWithContentType[headersWithContentType.length - 1] = "application/json; charset=utf-8";
        return post(
                uri,
                HttpRequest.BodyPublishers.ofString(ProtobufJsonUtils.toString(pojo), StandardCharsets.UTF_8),
                responseInfo -> new ProtobufSubscriber(responseClazz, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8).apply(responseInfo)),
                headersWithContentType
        );
    }

    public ResponseHelper<String> post(String uri, String body, String... headers) {
        return post(uri, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8), HttpResponse.BodyHandlers.ofString(), headers);
    }

    public <R> ResponseHelper<R> post(String uri, String body, Class<R> responseClazz, String... headers) {
        return post(
                uri,
                HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8),
                responseInfo -> new ProtobufSubscriber(responseClazz, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8).apply(responseInfo)),
                headers
        );
    }

    public ResponseHelper<String> postJson(String uri, String body, String... headers) {
        return postJson(uri, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8), HttpResponse.BodyHandlers.ofString(), headers);
    }

    public <R> ResponseHelper<R> postJson(String uri, String body, Class<R> responseClazz, String... headers) {
        return postJson(
                uri,
                HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8),
                responseInfo -> new ProtobufSubscriber(responseClazz, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8).apply(responseInfo)),
                headers
        );
    }

    public <R> ResponseHelper<R> post(String uri, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<R> bodyHandler, String... headers) {
        try {
            final HttpRequest.Builder builder = HttpRequest.newBuilder(toUri(uri)).POST(bodyPublisher);
            if (headers != null && headers.length > 0) {
                builder.headers(headers);
            }
            return new ResponseHelper<>(client.send(builder.build(), bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public <R> ResponseHelper<R> postJson(String uri, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<R> bodyHandler, String... headers) {
        try {
            final HttpRequest.Builder builder = HttpRequest.newBuilder(toUri(uri)).POST(bodyPublisher);
            if (headers != null && headers.length > 0) {
                builder.headers(headers);
            }
            builder.header("Content-Type", "application/json");
            return new ResponseHelper<>(client.send(builder.build(), bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public <R> ResponseHelper<R> postForm(String uri, HttpRequest.BodyPublisher bodyPublisher, HttpResponse.BodyHandler<R> bodyHandler) {
        try {
            HttpRequest request = HttpRequest.newBuilder(toUri(uri))
                    .POST(bodyPublisher)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public ResponseHelper<String> get(String uri, String... headersKeyAndValue) {
        return get(uri, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8), headersKeyAndValue);
    }

    static class ProtobufSubscriber<R> implements HttpResponse.BodySubscriber<R> {
        final Class<R> clazz;
        final HttpResponse.BodySubscriber<String> stringBodySubscriber;

        ProtobufSubscriber(Class<R> clazz, HttpResponse.BodySubscriber<String> stringBodySubscriber) {
            this.clazz = clazz;
            this.stringBodySubscriber = stringBodySubscriber;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            stringBodySubscriber.onSubscribe(subscription);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            stringBodySubscriber.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            stringBodySubscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            stringBodySubscriber.onComplete();
        }

        @Override
        public CompletionStage<R> getBody() {
            return stringBodySubscriber.getBody()
                    .thenApply(json -> {
                        if (json == null || json.isBlank()) {
                            return null;
                        }
                        try {
                            return ProtobufJsonUtils.toPojo(json, clazz);
                        } catch (Throwable e) {
                            LOG.error("Error while attempting to convert an alleged json string into a protobuf message instance: protobuf-class: '{}', string: '{}'", clazz.getName(), json);
                            throw e;
                        }
                    });
        }
    }

    public <R> ResponseHelper<R> get(String uri, Class<R> clazz) {
        return get(uri, responseInfo -> new ProtobufSubscriber(clazz, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8).apply(responseInfo)));
    }

    public <R> ResponseHelper<R> get(String uri, HttpResponse.BodyHandler<R> bodyHandler, String... headersKeyAndValue) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(toUri(uri)).GET();
            if (headersKeyAndValue.length > 1) {
                requestBuilder.headers(headersKeyAndValue);
            }
            HttpRequest request = requestBuilder.build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new RuntimeException(e);
        }
    }

    public ResponseHelper<String> delete(String uri) {
        return delete(uri, HttpResponse.BodyHandlers.ofString());
    }

    public <R> ResponseHelper<R> delete(String uri, HttpResponse.BodyHandler<R> bodyHandler) {
        try {
            HttpRequest request = HttpRequest.newBuilder(toUri(uri))
                    .DELETE()
                    .build();
            return new ResponseHelper<>(client.send(request, bodyHandler));
        } catch (Exception e) {
            LOG.error("Error: {}", captureStackTrace(e));
            throw new RuntimeException(e);
        }
    }
}
