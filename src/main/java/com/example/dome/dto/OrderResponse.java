package com.example.dome.dto;

import com.example.dome.model.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {
    private UUID orderId;
    private OrderStatus status;
    private String message;
    private Instant timestamp;
}
