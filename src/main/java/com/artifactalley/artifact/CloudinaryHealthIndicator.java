package com.artifactalley.artifact;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component("imageProvider")
@ConditionalOnProperty(name = "artifactalley.images.storage", havingValue = "cloudinary")
public class CloudinaryHealthIndicator implements HealthIndicator {
    private final CloudinaryArtifactImageStorage storage;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public CloudinaryHealthIndicator(CloudinaryArtifactImageStorage storage) { this.storage = storage; }

    @Override
    public Health health() {
        try {
            HttpRequest request = HttpRequest.newBuilder(storage.statusUri()).timeout(Duration.ofSeconds(5))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            return status < 500 ? Health.up().build() : Health.down().withDetail("status", "provider-http-" + status).build();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Health.down().withDetail("status", "interrupted").build();
        } catch (Exception exception) {
            return Health.down().withDetail("status", "unreachable").build();
        }
    }
}
