package com.artifactalley.artifact;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/** One page query plus one count query; card images and bid counts are projected without per-row lookups. */
@Repository
public class ArtifactSearchJdbcRepository {
    private static final String SELECT = """
            select a.id, a.title, a.category, a.era, a.starting_price, a.current_price,
                   a.closes_at, a.description, a.submitted_at,
                   coalesce(bc.bid_count, 0) bid_count,
                   (select min(ai.id) from artifact_images ai
                    where ai.artifact_id = a.id and ai.cover_image = true) cover_image_id
            from artifacts a
            left join (select artifact_id, count(*) bid_count from bids group by artifact_id) bc
              on bc.artifact_id = a.id
            """;
    private static final String COUNT = "select count(*) from artifacts a ";
    private final NamedParameterJdbcTemplate jdbc;

    public ArtifactSearchJdbcRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public SearchPage search(ArtifactSearchCriteria criteria, LocalDateTime now) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("status", ArtifactStatus.LIVE.name())
                .addValue("now", Timestamp.valueOf(now))
                .addValue("limit", criteria.size())
                .addValue("offset", (criteria.page() - 1L) * criteria.size());
        String where = where(criteria, now, parameters);
        String sql = SELECT + where + orderBy(criteria.sort()) + " limit :limit offset :offset";
        List<SearchRow> rows = jdbc.query(sql, parameters, (rs, rowNumber) -> new SearchRow(
                rs.getLong("id"), rs.getString("title"), Category.valueOf(rs.getString("category")),
                rs.getString("era"), rs.getBigDecimal("starting_price"), rs.getBigDecimal("current_price"),
                rs.getTimestamp("closes_at").toLocalDateTime(), rs.getString("description"),
                timestamp(rs.getTimestamp("submitted_at")), rs.getLong("bid_count"),
                nullableLong(rs.getObject("cover_image_id"))));
        Long total = jdbc.queryForObject(COUNT + where, parameters, Long.class);
        return new SearchPage(rows, total == null ? 0 : total);
    }

    private String where(ArtifactSearchCriteria criteria, LocalDateTime now, MapSqlParameterSource parameters) {
        StringBuilder sql = new StringBuilder(" where a.status = :status and a.closes_at > :now");
        if (criteria.query() != null) {
            parameters.addValue("query", "%" + escapeLike(criteria.query().toLowerCase(java.util.Locale.ROOT)) + "%");
            sql.append(" and (lower(a.title) like :query escape '!' or lower(a.description) like :query escape '!' ")
                    .append("or lower(a.era) like :query escape '!' or lower(a.category) like :query escape '!')");
        }
        if (criteria.category() != null) {
            parameters.addValue("category", criteria.category().name());
            sql.append(" and a.category = :category");
        }
        if (criteria.era() != null) {
            parameters.addValue("era", "%" + escapeLike(criteria.era().toLowerCase(java.util.Locale.ROOT)) + "%");
            sql.append(" and lower(a.era) like :era escape '!'");
        }
        if (criteria.minimumPrice() != null) {
            parameters.addValue("minimumPrice", criteria.minimumPrice());
            sql.append(" and a.current_price >= :minimumPrice");
        }
        if (criteria.maximumPrice() != null) {
            parameters.addValue("maximumPrice", criteria.maximumPrice());
            sql.append(" and a.current_price <= :maximumPrice");
        }
        if (criteria.endingWithin() != null) {
            LocalDateTime end = now.plusDays(criteria.endingWithin().getDays());
            parameters.addValue("endingBefore", Timestamp.valueOf(end));
            sql.append(" and a.closes_at <= :endingBefore");
        }
        return sql.toString();
    }

    private String orderBy(ArtifactSearchCriteria.SortOption sort) {
        return switch (sort) {
            case ENDING_SOON -> " order by a.closes_at asc, a.id asc";
            case NEWEST -> " order by coalesce(a.submitted_at, a.updated_at, a.closes_at) desc, a.id asc";
            case PRICE_ASC -> " order by a.current_price asc, a.id asc";
            case PRICE_DESC -> " order by a.current_price desc, a.id asc";
            case MOST_BIDS -> " order by coalesce(bc.bid_count, 0) desc, a.id asc";
        };
    }

    private String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private LocalDateTime timestamp(Timestamp value) { return value == null ? null : value.toLocalDateTime(); }
    private Long nullableLong(Object value) { return value == null ? null : ((Number) value).longValue(); }

    public record SearchPage(List<SearchRow> rows, long total) { }
    public record SearchRow(Long id, String title, Category category, String era,
                            BigDecimal startingPrice, BigDecimal currentPrice, LocalDateTime closesAt,
                            String description, LocalDateTime submittedAt, long bidCount, Long coverImageId) { }
}
