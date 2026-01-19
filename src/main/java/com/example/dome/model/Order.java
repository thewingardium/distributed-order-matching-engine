package com.example.dome.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class Order {

    @NonNull
    private final UUID orderId;

    @NonNull
    private final String symbol;

    @NonNull
    private final OrderSide side;

    @NonNull
    private final OrderType type;

    private final BigDecimal price;

    private final long quantity;

    @Builder.Default
    private long filledQuantity = 0;

    @NonNull
    private OrderStatus status;

    @NonNull
    private final Instant timestamp;

    /**
     * Basic validation of order state.
     * More complex validation (like price ticks, symbol existence) happens in OrderValidator.
     */
    public void validate() {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (price != null && price.compareTo(BigDecimal.ZERO) <= 0 && type != OrderType.MARKET) {
            throw new IllegalArgumentException("Price must be positive for non-market orders");
        }
        if (filledQuantity < 0) {
             throw new IllegalArgumentException("Filled quantity cannot be negative");
        }
    }
    
    public long getRemainingQuantity() {
        return quantity - filledQuantity;
    }

    public void fill(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Fill amount must be positive");
        }
        if (filledQuantity + amount > quantity) {
            throw new IllegalArgumentException("Cannot fill more than remaining quantity");
        }
        this.filledQuantity += amount;
        
        if (this.filledQuantity == this.quantity) {
            this.status = OrderStatus.FILLED;
        } else {
            this.status = OrderStatus.PARTIALLY_FILLED;
        }
    }
}
