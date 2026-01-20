package com.example.dome.engine;

import com.example.dome.model.Order;
import com.example.dome.model.Trade;

import java.util.List;

public interface MatchingAlgorithm {
    
    /**
     * Matches the incoming order against the resting orders in the book.
     * @param incomingOrder The aggressive order.
     * @param orderBook The order book for the symbol.
     * @return MatchResult containing trades and modified orders.
     */
    MatchResult match(Order incomingOrder, OrderBook orderBook);
}
