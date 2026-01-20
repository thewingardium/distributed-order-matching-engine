package com.example.dome.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class Trade {

    @NonNull
    private UUID tradeId;

    @NonNull
    private String symbol;

    @NonNull
    private UUID buyOrderId;

    @NonNull
    private UUID sellOrderId;

    @NonNull
    private BigDecimal price;

    private long quantity;

    @NonNull
    private Instant timestamp;
    
    // Optional: identifier for the matching event if needed for grouping
    private UUID matchEventId;
}
