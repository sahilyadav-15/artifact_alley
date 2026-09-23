package com.artifactalley.artifact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CloudinaryArtifactImageStorageTest {
    private HttpServer server;

    @AfterEach void stop() { if (server != null) server.stop(0); }

    @Test
    void uploadsReadsAndDeletesWithoutSendingApiSecret() throws Exception {
        AtomicReference<String> uploadBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1_1/demo/image/upload", exchange -> {
            uploadBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
            byte[] response = "{\"secure_url\":\"https://images.example/artifact.jpg\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/demo/image/upload/", exchange -> {
            byte[] response = new byte[]{1, 2, 3};
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/v1_1/demo/image/destroy", exchange -> {
            byte[] response = "{\"result\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        CloudinaryArtifactImageStorage storage = new CloudinaryArtifactImageStorage("demo", "api-key", "top-secret",
                Duration.ofSeconds(2), HttpClient.newHttpClient(), new ObjectMapper(), base, base);

        ArtifactImageStorage.StoredImage stored = storage.store(new byte[]{1, 2, 3}, "jpg", "image/jpeg");
        assertTrue(stored.storageKey().matches("cloudinary:artifact-alley/[0-9a-f-]{36}\\.jpg"));
        assertArrayEquals(new byte[]{1, 2, 3}, storage.open(stored.storageKey()).getInputStream().readAllBytes());
        assertDoesNotThrow(() -> storage.delete(stored.storageKey()));
        assertTrue(uploadBody.get().contains("api-key"));
        assertFalse(uploadBody.get().contains("top-secret"));
    }

    @Test
    void rejectsInvalidStorageKeysBeforeNetworkAccess() {
        CloudinaryArtifactImageStorage storage = new CloudinaryArtifactImageStorage("demo", "key", "secret",
                Duration.ofMillis(100), HttpClient.newHttpClient(), new ObjectMapper(),
                URI.create("http://127.0.0.1:1"), URI.create("http://127.0.0.1:1"));
        assertThrows(ArtifactOperationException.class, () -> storage.open("https://attacker.example/image.jpg"));
    }
}
