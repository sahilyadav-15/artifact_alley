package com.artifactalley.artifact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "artifactalley.images.storage", havingValue = "cloudinary")
public class CloudinaryArtifactImageStorage implements ArtifactImageStorage {
    private static final String PREFIX = "cloudinary:";
    private static final int MAX_DOWNLOAD_BYTES = 6 * 1024 * 1024;

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final Duration requestTimeout;
    private final HttpClient client;
    private final ObjectMapper json;
    private final URI apiBase;
    private final URI deliveryBase;

    @Autowired
    public CloudinaryArtifactImageStorage(
            @Value("${artifactalley.cloudinary.cloud-name:}") String cloudName,
            @Value("${artifactalley.cloudinary.api-key:}") String apiKey,
            @Value("${artifactalley.cloudinary.api-secret:}") String apiSecret,
            @Value("${artifactalley.cloudinary.connect-timeout:5s}") Duration connectTimeout,
            @Value("${artifactalley.cloudinary.request-timeout:20s}") Duration requestTimeout,
            ObjectMapper json) {
        this(cloudName, apiKey, apiSecret, requestTimeout,
                HttpClient.newBuilder().connectTimeout(connectTimeout).followRedirects(HttpClient.Redirect.NORMAL).build(), json,
                URI.create("https://api.cloudinary.com"), URI.create("https://res.cloudinary.com"));
    }

    CloudinaryArtifactImageStorage(String cloudName, String apiKey, String apiSecret, Duration requestTimeout,
                                   HttpClient client, ObjectMapper json, URI apiBase, URI deliveryBase) {
        this.cloudName = requireValue(cloudName, "CLOUDINARY_CLOUD_NAME");
        this.apiKey = requireValue(apiKey, "CLOUDINARY_API_KEY");
        this.apiSecret = requireValue(apiSecret, "CLOUDINARY_API_SECRET");
        this.requestTimeout = requestTimeout;
        this.client = client;
        this.json = json;
        this.apiBase = apiBase;
        this.deliveryBase = deliveryBase;
    }

    @Override
    public StoredImage store(byte[] content, String extension, String contentType) {
        String publicId = "artifact-alley/" + UUID.randomUUID();
        long timestamp = Instant.now().getEpochSecond();
        String signature = signature("public_id=" + publicId + "&timestamp=" + timestamp);
        String boundary = "ArtifactAlley" + UUID.randomUUID().toString().replace("-", "");
        byte[] body = multipart(boundary, content, extension, contentType, publicId, timestamp, signature);
        HttpRequest request = HttpRequest.newBuilder(uploadUri()).timeout(requestTimeout)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
        JsonNode response = sendJson(request, "upload");
        if (!response.path("secure_url").asText("").startsWith("https://")) {
            throw new ArtifactOperationException("Image provider returned an invalid delivery URL.");
        }
        return new StoredImage(PREFIX + publicId + "." + extension, contentType);
    }

    @Override
    public Resource open(String storageKey) {
        CloudKey key = parse(storageKey);
        HttpRequest request = HttpRequest.newBuilder(deliveryUri(key)).timeout(requestTimeout).GET().build();
        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 || response.body().length > MAX_DOWNLOAD_BYTES) {
                throw new ArtifactOperationException("Image file is unavailable.");
            }
            return new ByteArrayResource(response.body());
        } catch (IOException exception) {
            throw new ArtifactOperationException("Image provider is unavailable.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ArtifactOperationException("Image provider request was interrupted.");
        }
    }

    @Override
    public void delete(String storageKey) {
        CloudKey key = parse(storageKey);
        long timestamp = Instant.now().getEpochSecond();
        String signed = "invalidate=true&public_id=" + key.publicId() + "&timestamp=" + timestamp;
        String form = "public_id=" + encode(key.publicId()) + "&timestamp=" + timestamp
                + "&invalidate=true&api_key=" + encode(apiKey) + "&signature=" + signature(signed);
        HttpRequest request = HttpRequest.newBuilder(destroyUri()).timeout(requestTimeout)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        JsonNode response = sendJson(request, "delete");
        String result = response.path("result").asText();
        if (!"ok".equals(result) && !"not found".equals(result)) {
            throw new ArtifactOperationException("Image provider did not confirm deletion.");
        }
    }

    @Override
    public String publicUrl(Long imageId) { return "/artifact-images/" + imageId; }

    public URI statusUri() { return URI.create(deliveryBase + "/" + encodePath(cloudName) + "/image/upload/"); }

    private JsonNode sendJson(HttpRequest request, String operation) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ArtifactOperationException("Image provider " + operation + " failed with HTTP " + response.statusCode() + ".");
            }
            return json.readTree(response.body());
        } catch (IOException exception) {
            throw new ArtifactOperationException("Image provider " + operation + " failed.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ArtifactOperationException("Image provider request was interrupted.");
        }
    }

    private byte[] multipart(String boundary, byte[] content, String extension, String contentType,
                             String publicId, long timestamp, String signature) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            field(output, boundary, "api_key", apiKey);
            field(output, boundary, "timestamp", Long.toString(timestamp));
            field(output, boundary, "public_id", publicId);
            field(output, boundary, "signature", signature);
            output.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"image." + extension
                    + "\"\r\nContent-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(content);
            output.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return output.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private void field(ByteArrayOutputStream output, String boundary, String name, String value) throws IOException {
        output.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name
                + "\"\r\n\r\n" + value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private String signature(String parameters) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1")
                    .digest((parameters + apiSecret).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException("SHA-1 is unavailable.", exception); }
    }

    private CloudKey parse(String storageKey) {
        if (storageKey == null || !storageKey.matches("cloudinary:artifact-alley/[0-9a-f-]{36}\\.(jpg|png)")) {
            throw new ArtifactOperationException("Invalid image reference.");
        }
        String value = storageKey.substring(PREFIX.length());
        int dot = value.lastIndexOf('.');
        return new CloudKey(value.substring(0, dot), value.substring(dot + 1));
    }

    private URI uploadUri() { return URI.create(apiBase + "/v1_1/" + encodePath(cloudName) + "/image/upload"); }
    private URI destroyUri() { return URI.create(apiBase + "/v1_1/" + encodePath(cloudName) + "/image/destroy"); }
    private URI deliveryUri(CloudKey key) {
        return URI.create(deliveryBase + "/" + encodePath(cloudName) + "/image/upload/"
                + encodePath(key.publicId()) + "." + key.extension());
    }
    private static String encode(String value) { return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String encodePath(String value) { return value.replace("/", "%2F").replace(" ", "%20"); }
    private static String requireValue(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required.");
        return value;
    }
    private record CloudKey(String publicId, String extension) { }
}
