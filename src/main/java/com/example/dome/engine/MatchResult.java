package com.example.dome.engine;

import com.example.dome.model.Order;
import com.example.dome.model.Trade;
import java.util.List;

public record MatchResult(List<Trade> trades, List<Order> modifiedOrders) {
}
