package com.example.dome.event;

import com.example.dome.model.Trade;
import java.time.Instant;

/**
 * Event representing a completed trade.
 * Wraps the Trade entity with event-specific metadata if needed.
 */
public record TradeEvent(Trade trade, Instant eventTimestamp) {
    public TradeEvent(Trade trade) {
        this(trade, Instant.now());
    }
}
