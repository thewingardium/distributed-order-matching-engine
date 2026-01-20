package com.example.dome.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OrderBookDTO {
    private String symbol;
    private List<LevelDTO> bids;
    private List<LevelDTO> asks;
    private long timestamp;

    @Data
    @Builder
    public static class LevelDTO {
        private BigDecimal price;
        private long quantity;
        private int orderCount;
    }
}
