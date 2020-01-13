package no.ssb.testing.helidon;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResponseHelper<T> {

    private final HttpResponse<T> response;
    private final T body;

    public ResponseHelper(HttpResponse<T> response) {
        this.response = response;
        this.body = response.body();
    }

    public HttpResponse<T> response() {
        return response;
    }

    public T body() {
        return body;
    }

    public ResponseHelper<T> expectAnyOf(int... anyOf) {
        int matchingStatusCode = -1;
        for (int statusCode : anyOf) {
            if (response.statusCode() == statusCode) {
                matchingStatusCode = statusCode;
            }
        }
        assertTrue(matchingStatusCode != -1, "Actual statusCode was " + response.statusCode() + " message: " + String.valueOf(body));
        return this;
    }

    public ResponseHelper<T> expect403Forbidden() {
        assertEquals(HttpURLConnection.HTTP_FORBIDDEN, response.statusCode(), String.valueOf(body));
        return this;
    }

    public ResponseHelper<T> expect400BadRequest() {
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, response.statusCode(), String.valueOf(body));
        return this;
    }

    public ResponseHelper<T> expect404NotFound() {
        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.statusCode(), String.valueOf(body));
        return this;
    }

    public ResponseHelper<T> expect200Ok() {
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode(), String.valueOf(body));
        return this;
    }

    public ResponseHelper<T> expect201Created() {
        assertEquals(HttpURLConnection.HTTP_CREATED, response.statusCode(), String.valueOf(body));
        return this;
    }

    public ResponseHelper<T> expect204NoContent() {
        assertEquals(HttpURLConnection.HTTP_NO_CONTENT, response.statusCode(), String.valueOf(body));
        return this;
    }
}
