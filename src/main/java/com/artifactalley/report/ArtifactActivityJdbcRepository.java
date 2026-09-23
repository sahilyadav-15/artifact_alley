package com.artifactalley.report;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Small direct-JDBC example; core auction writes deliberately remain in transactional JPA services. */
@Repository
public class ArtifactActivityJdbcRepository {
    private static final String SQL = """
            select a.id, a.title, a.status, a.submitted_at, count(b.id) bid_count
            from artifacts a left join bids b on b.artifact_id = a.id
            where a.submitted_at >= ? and a.submitted_at < ?
            group by a.id, a.title, a.status, a.submitted_at
            order by a.submitted_at asc, a.id asc
            """;
    private final DataSource dataSource;

    public ArtifactActivityJdbcRepository(DataSource dataSource) { this.dataSource = dataSource; }

    public List<ArtifactActivity> findBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new IllegalArgumentException("The activity range must have a start before its end.");
        }
        List<ArtifactActivity> results = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL)) {
            statement.setTimestamp(1, Timestamp.valueOf(from));
            statement.setTimestamp(2, Timestamp.valueOf(to));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(new ArtifactActivity(resultSet.getLong("id"), resultSet.getString("title"),
                            resultSet.getString("status"), resultSet.getTimestamp("submitted_at").toLocalDateTime(),
                            resultSet.getLong("bid_count")));
                }
            }
            return results;
        } catch (SQLException exception) {
            throw new IllegalStateException("The artifact activity report could not be read.", exception);
        }
    }

    public record ArtifactActivity(Long artifactId, String title, String status,
                                   LocalDateTime submittedAt, long bidCount) { }
}
