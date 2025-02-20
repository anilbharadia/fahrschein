package org.zalando.fahrschein.http.jdk11;

import org.zalando.fahrschein.http.api.Headers;
import org.zalando.fahrschein.http.api.HeadersImpl;
import org.zalando.fahrschein.http.api.Response;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.zip.GZIPInputStream;

final class JavaNetResponse implements Response {

    private final HttpResponse<InputStream> r;

    JavaNetResponse(HttpResponse<InputStream> r) {
        this.r = r;
    }

    @Override
    public int getStatusCode() throws IOException {
        return this.r.statusCode();
    }

    @Override
    public String getStatusText() throws IOException {
        switch(this.r.statusCode()) {
            case (200): return "OK";
            case (201): return "Created";
            case (202): return "Accepted";
            case (203): return "Non-Authoritative Information";
            case (204): return "No Content";
            case (205): return "Reset Content";
            case (206): return "Partial Content";
            case (207): return "Partial Update OK";
            case (300): return "Multiple Choices";
            case (301): return "Moved Permanently";
            case (302): return "Moved Temporarily";
            case (303): return "See Other";
            case (304): return "Not Modified";
            case (305): return "Use Proxy";
            case (307): return "Temporary Redirect";
            case (400): return "Bad Request";
            case (401): return "Unauthorized";
            case (402): return "Payment Required";
            case (403): return "Forbidden";
            case (404): return "Not Found";
            case (405): return "Method Not Allowed";
            case (406): return "Not Acceptable";
            case (407): return "Proxy Authentication Required";
            case (408): return "Request Timeout";
            case (409): return "Conflict";
            case (410): return "Gone";
            case (411): return "Length Required";
            case (412): return "Precondition Failed";
            case (413): return "Request Entity Too Large";
            case (414): return "Request-URI Too Long";
            case (415): return "Unsupported Media Type";
            case (416): return "Requested Range Not Satisfiable";
            case (417): return "Expectation Failed";
            case (418): return "Reauthentication Required";
            case (419): return "Proxy Reauthentication Required";
            case (422): return "Unprocessable Entity";
            case (423): return "Locked";
            case (424): return "Failed Dependency";
            case (500): return "Server Error";
            case (501): return "Not Implemented";
            case (502): return "Bad Gateway";
            case (503): return "Service Unavailable";
            case (504): return "Gateway Timeout";
            case (505): return "HTTP Version Not Supported";
            case (507): return "Insufficient Storage";
            default: return "";
        }
    }

    @Override
    public Headers getHeaders() {
        return new JavaNetHeadersDelegate(this.r.headers());
    }

    @Override
    public InputStream getBody() throws IOException {
        if (this.getHeaders().get(Headers.CONTENT_ENCODING).contains("gzip")) {
            return new GZIPInputStream(r.body());
        }
        return r.body();
    }

    @Override
    public void close() {
        try {
            r.body().close();
        } catch (Exception e) {
            // ignore
        }
    }

}
