package com.example.dome.engine;

import com.example.dome.model.Order;
import com.example.dome.model.OrderSide;
import com.example.dome.model.OrderType;
import com.example.dome.model.Trade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FifoMatchingAlgorithm implements MatchingAlgorithm {

    @Override
    public MatchResult match(Order incoming, OrderBook book) {
        List<Trade> trades = new ArrayList<>();
        List<Order> modifiedOrders = new ArrayList<>();
        
        // Incoming order is always modified (potentially) or at least needs saving if it enters book.
        // Actually, MatchingEngine persists incoming. But for completeness, we can add it here or handle in Engine.
        // Let's only add *resting* orders here to avoid confusion/duplication, or add all.
        // If we add all, we must be careful not to save twice or assume engine handles incoming.
        // Let's add ONLY resting orders that were modified. Incoming is handled by caller.
        
        while (incoming.getRemainingQuantity() > 0) {
            PriceLevel bestLevel = (incoming.getSide() == OrderSide.BUY) ? book.getBestAsk() : book.getBestBid();
            
            if (bestLevel == null) {
                break; // No liquidity
            }
            
            // Check Price Condition
            if (!canMatch(incoming, bestLevel.getPrice())) {
                break;
            }
            
            // Iterate orders in this level (FIFO)
            while (incoming.getRemainingQuantity() > 0 && !bestLevel.isEmpty()) {
                Order resting = bestLevel.peek();
                if (resting == null) break; 
                
                // Skip cancelled orders
                if (resting.getStatus() == com.example.dome.model.OrderStatus.CANCELED) {
                    bestLevel.poll(); 
                    continue;
                }
                
                // Match logic
                long quantityToTrade = Math.min(incoming.getRemainingQuantity(), resting.getRemainingQuantity());
                
                if (quantityToTrade > 0) {
                    Trade trade = Trade.builder()
                            .tradeId(UUID.randomUUID())
                            .symbol(incoming.getSymbol())
                            .buyOrderId(incoming.getSide() == OrderSide.BUY ? incoming.getOrderId() : resting.getOrderId())
                            .sellOrderId(incoming.getSide() == OrderSide.SELL ? incoming.getOrderId() : resting.getOrderId())
                            .price(bestLevel.getPrice()) 
                            .quantity(quantityToTrade)
                            .timestamp(Instant.now())
                            .build();
                    
                    trades.add(trade);
                    
                    // Update orders
                    incoming.fill(quantityToTrade);
                    resting.fill(quantityToTrade);
                    
                    // Update PriceLevel quantity
                    bestLevel.reduceTotalQuantity(quantityToTrade);
                    
                    // Track modified resting order
                    modifiedOrders.add(resting);
                }
                
                if (resting.getRemainingQuantity() == 0) {
                    bestLevel.poll(); // Remove fully filled
                }
            }
            
            if (bestLevel.isEmpty()) {
                 book.removeLevel(bestLevel.getPrice());
            }
        }
        
        return new MatchResult(trades, modifiedOrders);
    }
    
    private boolean canMatch(Order incoming, BigDecimal restingPrice) {
        if (incoming.getType() == OrderType.MARKET) return true;
        
        if (incoming.getSide() == OrderSide.BUY) {
            // Buy Limit >= Resting Sell Price
            return incoming.getPrice().compareTo(restingPrice) >= 0;
        } else {
            // Sell Limit <= Resting Buy Price
            return incoming.getPrice().compareTo(restingPrice) <= 0;
        }
    }
}
