package com.example.dome.dto;

import com.example.dome.model.OrderSide;
import com.example.dome.model.OrderType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderRequest {

    @NotNull(message = "Symbol is required")
    private String symbol;

    @NotNull(message = "Side is required")
    private OrderSide side;

    @NotNull(message = "Type is required")
    private OrderType type;

    // Price can be null for MARKET orders
    private BigDecimal price;

    @Positive(message = "Quantity must be positive")
    private long quantity;
}
