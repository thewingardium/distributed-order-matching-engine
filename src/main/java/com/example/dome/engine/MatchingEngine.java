package com.example.dome.engine;

import com.example.dome.event.EventProcessor;
import com.example.dome.event.TradeEvent;
import com.example.dome.model.Order;
import com.example.dome.model.Trade;
import com.example.dome.persistence.OrderDao;
import jakarta.annotation.PostConstruct;


import java.util.List;
import java.util.UUID;

public class MatchingEngine {

    private final String symbol;
    private final OrderBook orderBook;
    
    // In Week 2, we can hardcode the algorithm or inject it.
    private final MatchingAlgorithm matchingAlgorithm = new FifoMatchingAlgorithm();
    private final EventProcessor eventProcessor;
    private final OrderDao orderDao;
    public MatchingEngine(String symbol, EventProcessor eventProcessor, OrderDao orderDao) {
        this.symbol = symbol;
        this.orderBook = new OrderBook(symbol);
        this.eventProcessor = eventProcessor;
        this.orderDao = orderDao;
    }

    public OrderBook getOrderBook() {
        return orderBook;
    }

    /**
     * Async/Disruptor compatible matching.
     * Does NOT persist to DB.
     * @return MatchResult.
     */
    public MatchResult match(Order order) {
         if (!order.getSymbol().equals(this.symbol)) {
             throw new IllegalArgumentException("Order symbol mismatch. Engine is " + symbol + " but order is " + order.getSymbol());
        }

        // Match Logic
        MatchResult matchResult = matchingAlgorithm.match(order, orderBook);
        
        // Logic for book update
        if (order.getRemainingQuantity() > 0) {
             if (order.getType() == com.example.dome.model.OrderType.LIMIT) {
                 orderBook.addOrder(order);
             }
        }
        
        // Return full result for Persistence Handler
        return matchResult;
    }

    // Keep old method for backward compatibility / tests until full migration
    public List<Trade> processOrder(Order order) {
        MatchResult result = match(order);
        List<Trade> trades = result.trades();
        
        // Persist modified resting orders
        for (Order modifiedOrder : result.modifiedOrders()) {
            orderDao.save(modifiedOrder);
        }
        
        // Persist (Synchronous fallback)
        orderDao.save(order);

        for (Trade trade : trades) {
             eventProcessor.onTrade(new TradeEvent(trade));
        }
        
        var bestBid = orderBook.getBestBid();
        var bestAsk = orderBook.getBestAsk();
        eventProcessor.onBookUpdate(symbol, 
            bestBid != null ? bestBid.getPrice() : null, 
            bestAsk != null ? bestAsk.getPrice() : null);
            
        return trades;
    }

    public void cancelOrder(UUID orderId) {
        Order order = orderDao.findById(orderId.toString());
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        
        if (!order.getSymbol().equals(this.symbol)) {
             throw new IllegalArgumentException("Order symbol mismatch. Engine is " + symbol + " but order is " + order.getSymbol());
        }
        
        orderBook.cancelOrder(order);
        
        order.setStatus(com.example.dome.model.OrderStatus.CANCELED);
        orderDao.save(order);
        
        // Update Cache
        // Update Cache & WebSocket via EventProcessor
        var bestBid = orderBook.getBestBid();
        var bestAsk = orderBook.getBestAsk();
        eventProcessor.onBookUpdate(symbol, 
            bestBid != null ? bestBid.getPrice() : null, 
            bestAsk != null ? bestAsk.getPrice() : null);
    }
}
