package com.artifactalley.report;

import com.artifactalley.artifact.*;
import com.artifactalley.user.Role;
import com.artifactalley.user.User;
import com.artifactalley.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JdbcReportingIntegrationTest {
    @Autowired AuctionReportJdbcRepository reportRepository;
    @Autowired ArtifactActivityJdbcRepository activityRepository;
    @Autowired ArtifactRepository artifacts;
    @Autowired UserRepository users;

    @Test
    void springJdbcAggregatesAndDirectJdbcMapsPreparedDateRange() {
        String suffix = Long.toHexString(System.nanoTime());
        User seller = users.save(new User("Report Seller", "report-" + suffix + "@test.invalid", "hash", Role.SELLER));
        LocalDateTime submitted = LocalDateTime.now().minusHours(1).withNano(0);
        Artifact artifact = artifacts.saveAndFlush(new Artifact("JDBC report fixture", Category.MANUSCRIPT, "1700",
                new BigDecimal("1200"), LocalDateTime.now().plusDays(2), "Reporting fixture", seller,
                seller.getEmail(), ArtifactStatus.PENDING_APPROVAL, submitted));

        AuctionReport report = reportRepository.summary(25);
        assertThat(report.artifactCounts()).anyMatch(value -> value.status().equals("PENDING_APPROVAL") && value.count() > 0);
        assertThat(report.sellerListings()).anyMatch(value -> value.userId().equals(seller.getId()));

        assertThat(activityRepository.findBetween(submitted.minusMinutes(1), submitted.plusMinutes(1)))
                .anySatisfy(value -> {
                    assertThat(value.artifactId()).isEqualTo(artifact.getId());
                    assertThat(value.title()).isEqualTo("JDBC report fixture");
                    assertThat(value.bidCount()).isZero();
                });
        assertThat(activityRepository.findBetween(submitted.plusDays(10), submitted.plusDays(11))).isEmpty();
    }
}
