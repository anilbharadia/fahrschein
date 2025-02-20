package org.zalando.fahrschein.http.spring;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequest;
import org.zalando.fahrschein.http.api.ContentEncoding;
import org.zalando.fahrschein.http.api.Headers;
import org.zalando.fahrschein.http.api.Request;
import org.zalando.fahrschein.http.api.Response;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;

class RequestAdapter implements Request {
    private final ClientHttpRequest clientHttpRequest;
    private final ContentEncoding contentEncoding;

    RequestAdapter(ClientHttpRequest clientHttpRequest, ContentEncoding contentEncoding) {
        this.clientHttpRequest = clientHttpRequest;
        this.contentEncoding = contentEncoding;
    }

    @Override
    public String getMethod() {
        return clientHttpRequest.getMethod().name();
    }

    @Override
    public URI getURI() {
        return clientHttpRequest.getURI();
    }

    @Override
    public Headers getHeaders() {
        return new HeadersAdapter(clientHttpRequest.getHeaders());
    }

    @Override
    public OutputStream getBody() throws IOException {
        if (this.contentEncoding.isSupported(getMethod())) {
            // probably premature optimization, but we're omitting the unnecessary
            // "Content-Encoding: identity" header
            if (ContentEncoding.IDENTITY != this.contentEncoding) {
                clientHttpRequest.getHeaders().set(HttpHeaders.CONTENT_ENCODING, contentEncoding.value());
            }
            return this.contentEncoding.wrap(clientHttpRequest.getBody());
        }
        return clientHttpRequest.getBody();
    }

    @Override
    public Response execute() throws IOException {
        return new ResponseAdapter(clientHttpRequest.execute());
    }
}
