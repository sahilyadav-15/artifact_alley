package com.artifactalley.report;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/** Read-only aggregate queries. Spring owns connections and maps each ResultSet row through the lambdas. */
@Repository
public class AuctionReportJdbcRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public AuctionReportJdbcRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public AuctionReport summary(int activityLimit) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("limit", activityLimit);
        List<AuctionReport.StatusCount> statuses = jdbc.query(
                "select status, count(*) artifact_count from artifacts group by status order by status",
                (rs, row) -> new AuctionReport.StatusCount(rs.getString("status"), rs.getLong("artifact_count")));
        long totalBids = value("select count(*) from bids", Long.class, 0L);
        BigDecimal soldValue = value("select coalesce(sum(current_price), 0) from artifacts where status = 'SOLD'",
                BigDecimal.class, BigDecimal.ZERO);
        BigDecimal average = value("select coalesce(avg(current_price), 0) from artifacts where status = 'SOLD'",
                BigDecimal.class, BigDecimal.ZERO);
        List<AuctionReport.ActiveAuction> active = jdbc.query("""
                select a.id, a.title, count(b.id) bid_count
                from artifacts a left join bids b on b.artifact_id = a.id
                group by a.id, a.title
                order by bid_count desc, a.id asc
                limit :limit
                """, parameters, (rs, row) -> new AuctionReport.ActiveAuction(
                rs.getLong("id"), rs.getString("title"), rs.getLong("bid_count")));
        List<AuctionReport.ParticipantCount> sellers = jdbc.query("""
                select u.id, u.name, count(a.id) item_count
                from app_users u join artifacts a on a.seller_id = u.id
                group by u.id, u.name order by item_count desc, u.id asc
                limit :limit
                """, parameters, (rs, row) -> new AuctionReport.ParticipantCount(
                rs.getLong("id"), rs.getString("name"), rs.getLong("item_count")));
        List<AuctionReport.ParticipantCount> bidders = jdbc.query("""
                select u.id, u.name, count(b.id) item_count
                from app_users u join bids b on b.bidder_id = u.id
                group by u.id, u.name order by item_count desc, u.id asc
                limit :limit
                """, parameters, (rs, row) -> new AuctionReport.ParticipantCount(
                rs.getLong("id"), rs.getString("name"), rs.getLong("item_count")));
        return new AuctionReport(statuses, totalBids, soldValue, average, active, sellers, bidders);
    }

    private <T> T value(String sql, Class<T> type, T fallback) {
        T value = jdbc.getJdbcTemplate().queryForObject(sql, type);
        return value == null ? fallback : value;
    }
}
