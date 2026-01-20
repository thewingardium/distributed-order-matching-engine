package com.example.dome.persistence;

import com.example.dome.model.Trade;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public class TradeRepository {

    private final JdbcTemplate jdbcTemplate;

    public TradeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initTable();
    }

    private void initTable() {
        // Basic table creation.
        // In a real production setup, use Flyway or Liquibase.
        String sql = """
            CREATE TABLE IF NOT EXISTS trades (
                trade_id VARCHAR(36) PRIMARY KEY,
                symbol VARCHAR(20) NOT NULL,
                buy_order_id VARCHAR(36) NOT NULL,
                sell_order_id VARCHAR(36) NOT NULL,
                price DECIMAL(20, 8) NOT NULL,
                quantity BIGINT NOT NULL,
                timestamp TIMESTAMP NOT NULL
            );
            """;
        // Note: For TimescaleDB, we would add: SELECT create_hypertable('trades', 'timestamp');
        // keeping it simple for now to avoid errors if extension is missing.
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            // Log/Ignore if connection fails during dev (e.g. no DB running)
            // But we should probably throw if strict.
            System.err.println("Warning: Could not initialize DB table: " + e.getMessage());
        }
    }

    public void save(Trade trade) {
        String sql = """
            INSERT INTO trades (trade_id, symbol, buy_order_id, sell_order_id, price, quantity, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        jdbcTemplate.update(sql,
                trade.getTradeId().toString(),
                trade.getSymbol(),
                trade.getBuyOrderId().toString(),
                trade.getSellOrderId().toString(),
                trade.getPrice(),
                trade.getQuantity(),
                Timestamp.from(trade.getTimestamp())
        );
    }
    
    public java.util.List<Trade> findAll() {
        String sql = "SELECT * FROM trades ORDER BY timestamp DESC LIMIT 50";
        return jdbcTemplate.query(sql, (rs, rowNum) -> Trade.builder()
                .tradeId(java.util.UUID.fromString(rs.getString("trade_id")))
                .symbol(rs.getString("symbol"))
                .buyOrderId(java.util.UUID.fromString(rs.getString("buy_order_id")))
                .sellOrderId(java.util.UUID.fromString(rs.getString("sell_order_id")))
                .price(rs.getBigDecimal("price"))
                .quantity(rs.getLong("quantity"))
                .timestamp(rs.getTimestamp("timestamp").toInstant())
                .build());
    }
}
