package com.artifactalley.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class SecurityAuditService {
    private static final Logger log = LoggerFactory.getLogger("SECURITY_AUDIT");
    private final MeterRegistry metrics;

    public SecurityAuditService(MeterRegistry metrics) { this.metrics = metrics; }

    public void record(String event, Long actorId, Long targetId, String result) {
        String safeEvent = safe(event);
        String safeResult = safe(result);
        log.info("event={} actorId={} targetId={} result={} requestId={}", safeEvent, actorId, targetId,
                safeResult, safe(MDC.get("requestId")));
        metrics.counter("artifactalley.security.events", "event", safeEvent, "result", safeResult).increment();
    }

    private String safe(String value) {
        if (value == null) return "-";
        return value.replaceAll("[^A-Za-z0-9_.-]", "_");
    }
}
