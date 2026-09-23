package com.artifactalley.config;

import com.artifactalley.artifact.ArtifactImageStorage;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class OperationalStartupLogger {
    private static final Logger log = LoggerFactory.getLogger(OperationalStartupLogger.class);
    private final Environment environment;
    private final Flyway flyway;
    private final ArtifactImageStorage images;

    public OperationalStartupLogger(Environment environment, Flyway flyway, ArtifactImageStorage images) {
        this.environment = environment;
        this.flyway = flyway;
        this.images = images;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ready() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) profiles = environment.getDefaultProfiles();
        var current = flyway.info().current();
        log.info("Application ready profiles={} flywayVersion={} imageStorage={}", Arrays.toString(profiles),
                current == null ? "none" : current.getVersion(), images.getClass().getSimpleName());
    }
}
