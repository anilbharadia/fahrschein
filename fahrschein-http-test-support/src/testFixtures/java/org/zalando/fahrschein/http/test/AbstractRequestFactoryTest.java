package org.zalando.fahrschein.http.test;

import com.github.luben.zstd.ZstdInputStream;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.zalando.fahrschein.http.api.ContentEncoding;
import org.zalando.fahrschein.http.api.ContentType;
import org.zalando.fahrschein.http.api.Headers;
import org.zalando.fahrschein.http.api.Request;
import org.zalando.fahrschein.http.api.RequestFactory;
import org.zalando.fahrschein.http.api.Response;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractRequestFactoryTest {

    public abstract RequestFactory defaultRequestFactory(ContentEncoding contentEncoding);

    protected static HttpServer server;
    protected static URI serverAddress;

    @BeforeAll
    public static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 1);
        serverAddress = URI.create("http://localhost:" + server.getAddress().getPort());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        server.setExecutor(executor);
        server.start();
    }

    @Captor
    public ArgumentCaptor<HttpExchange> exchangeCaptor;

    @Test
    public void testGzippedResponseBody() throws IOException {
        // given
        String expectedResponse = "{}";
        HttpHandler spy = Mockito.spy(new GzippedResponseContentHandler(expectedResponse));
        server.createContext("/gzipped", spy);

        // when
        final RequestFactory f = defaultRequestFactory(ContentEncoding.IDENTITY);
        Request r = f.createRequest(serverAddress.resolve("/gzipped"), "GET");
        Response executed = r.execute();
        String actualResponse = readStream(executed.getBody());

        // then
        Mockito.verify(spy).handle(exchangeCaptor.capture());
        HttpExchange capturedArgument = exchangeCaptor.getValue();
        assertThat("accept-encoding header", capturedArgument.getRequestHeaders().getFirst("accept-encoding"), containsString("gzip"));
        assertThat("no content-encoding header", capturedArgument.getRequestHeaders().get(Headers.CONTENT_ENCODING), nullValue());
        assertEquals(URI.create("/gzipped"), capturedArgument.getRequestURI());
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void testEncodedRequestBody() throws IOException {
        for(ContentEncoding encoding : ContentEncoding.values()) {
            doTestEncodedRequestBody(encoding);
        }
    }

    void doTestEncodedRequestBody(ContentEncoding encoding) throws IOException {
        // given
        String requestBody = "{}";
        String responseBody = "{}";
        SimpleRequestResponseContentHandler spy = Mockito.spy(new SimpleRequestResponseContentHandler(responseBody));
        String requestPath = "/post-" + encoding.value();
        server.createContext(requestPath, spy);

        // when
        final RequestFactory f = defaultRequestFactory(encoding);
        Request r = f.createRequest(serverAddress.resolve(requestPath), "POST");
        r.getHeaders().setContentType(ContentType.APPLICATION_JSON);
        try (final OutputStream body = r.getBody()) {
            body.write(requestBody.getBytes());
        }
        Response executed = r.execute();
        String actualResponse = readStream(executed.getBody());

        // then
        Mockito.verify(spy).handle(exchangeCaptor.capture());
        HttpExchange capturedArgument = exchangeCaptor.getValue();
        assertEquals("POST", capturedArgument.getRequestMethod());
        assertEquals(URI.create(requestPath), capturedArgument.getRequestURI());
        if (encoding == ContentEncoding.IDENTITY) {
            assertThat("no content-encoding header", capturedArgument.getRequestHeaders().get(Headers.CONTENT_ENCODING), is(nullValue()));
        } else {
            assertThat("content-encoding header", capturedArgument.getRequestHeaders().get(Headers.CONTENT_ENCODING), equalTo(Arrays.asList(encoding.value())));
        }
        assertEquals(requestBody, spy.getRequestBody());
        assertEquals(responseBody, actualResponse);
    }

    static String readStream(InputStream stream) throws IOException {
        String res = new BufferedReader(
                new InputStreamReader(stream, UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
        stream.close();
        return res;
    }

    private static class SimpleRequestResponseContentHandler implements HttpHandler {

        private String requestBody;
        private final String responseBody;

        SimpleRequestResponseContentHandler(String responseBody) {
            this.responseBody = responseBody;
        }

        public String getRequestBody() {
            return requestBody;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (exchange.getRequestHeaders().containsKey(Headers.CONTENT_ENCODING) && exchange.getRequestHeaders().get(Headers.CONTENT_ENCODING).contains("gzip")) {
                    requestBody = readStream(new GZIPInputStream(exchange.getRequestBody()));
                } else if (exchange.getRequestHeaders().containsKey(Headers.CONTENT_ENCODING) && exchange.getRequestHeaders().get(Headers.CONTENT_ENCODING).contains("zstd")) {
                    requestBody = readStream(new ZstdInputStream(exchange.getRequestBody()));
                } else {
                    requestBody = readStream(exchange.getRequestBody());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            byte[] bytes = responseBody.getBytes(UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream responseBody = exchange.getResponseBody();
            responseBody.write(bytes);
            responseBody.close();
        }
    }

    private static class GzippedResponseContentHandler implements HttpHandler {

        private final byte[] rawResponse;

        GzippedResponseContentHandler(String response) throws IOException {
            byte[] stringResponse = response.getBytes(UTF_8);
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            GZIPOutputStream zipStream = new GZIPOutputStream(byteStream);
            zipStream.write(stringResponse);
            zipStream.close();
            this.rawResponse = byteStream.toByteArray();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set(Headers.CONTENT_ENCODING, "gzip");
            exchange.sendResponseHeaders(200, rawResponse.length);
            OutputStream responseBody = exchange.getResponseBody();
            responseBody.write(rawResponse);
            responseBody.close();
        }
    }
}
