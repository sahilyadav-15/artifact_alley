package com.artifactalley;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorExposureTest {
    @Autowired TestRestTemplate http;

    @Test
    void probesAreAvailableWithoutDetailsAndMetricsEndpointIsClosed() throws Exception {
        var liveness = http.getForEntity("/actuator/health/liveness", String.class);
        assertEquals(200, liveness.getStatusCode().value());
        assertEquals("{\"status\":\"UP\"}", liveness.getBody());
        var readiness = http.getForEntity("/actuator/health/readiness", String.class);
        assertEquals(200, readiness.getStatusCode().value());
        assertEquals("{\"status\":\"UP\"}", readiness.getBody());
        assertEquals(404, http.getForEntity("/actuator/metrics", String.class).getStatusCode().value());
    }
}
