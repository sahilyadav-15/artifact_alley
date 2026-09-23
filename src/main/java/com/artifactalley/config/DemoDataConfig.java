package com.artifactalley.config;

import com.artifactalley.artifact.*;
import com.artifactalley.user.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Clock;

@Configuration
public class DemoDataConfig {
    @Bean
    CommandLineRunner loadDemoArtifacts(ArtifactRepository repository, UserService userService, Clock clock) {
        return args -> {
            userService.ensureInitialAdmin();
            if (repository.count() == 0) {
                LocalDateTime now = LocalDateTime.now(clock);
                repository.save(new Artifact("Chola Bronze Nataraja", Category.SCULPTURE, "11th–12th century",
                        new BigDecimal("85000.00"), LocalDateTime.now(clock).plusDays(4),
                        "A finely detailed bronze representation of Shiva as Nataraja.", null, null, ArtifactStatus.LIVE, now));
                repository.save(new Artifact("Mughal Miniature Painting", Category.PAINTING, "18th century",
                        new BigDecimal("42000.00"), LocalDateTime.now(clock).plusDays(2),
                        "Courtly miniature artwork with natural pigments on handmade paper.", null, null, ArtifactStatus.LIVE, now));
                repository.save(new Artifact("British India Silver Rupee", Category.COIN, "1911",
                        new BigDecimal("6000.00"), LocalDateTime.now(clock).plusDays(6),
                        "Silver rupee from the reign of George V, offered for collectors.", null, null, ArtifactStatus.LIVE, now));
            }
        };
    }
}
