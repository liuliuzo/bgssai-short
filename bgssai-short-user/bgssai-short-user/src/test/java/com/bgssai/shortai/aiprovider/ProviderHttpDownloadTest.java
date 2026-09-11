package com.bgssai.shortai.aiprovider;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ProviderHttpDownloadTest {
    @Test void authenticatesInitialDownloadAndStripsCredentialsOnCrossOriginRedirect() throws Exception {
        var source = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var destination = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var initialKey = new AtomicReference<String>(); var redirectedKey = new AtomicReference<String>();
        destination.createContext("/video", exchange -> {
            redirectedKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            byte[] bytes = "video-fixture".getBytes(); exchange.sendResponseHeaders(200,bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        source.createContext("/download", exchange -> {
            initialKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            exchange.getResponseHeaders().set("Location", "http://127.0.0.1:"+destination.getAddress().getPort()+"/video");
            exchange.sendResponseHeaders(302,-1); exchange.close();
        });
        source.start(); destination.start();
        try {
            byte[] bytes = new ProviderHttp.JdkClient().getBytes("http://127.0.0.1:"+source.getAddress().getPort()+"/download",Map.of("x-goog-api-key","synthetic-test-key"),15);
            assertEquals("video-fixture",new String(bytes)); assertEquals("synthetic-test-key",initialKey.get()); assertNull(redirectedKey.get());
        } finally { source.stop(0); destination.stop(0); }
    }
}
