package com.example.dome.event;

import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Processes system events.
 * Currently a skeleton that logs events.
 * In future phases, this will dispatch to persistence layers or messaging buses.
 */
@Component
public class EventProcessor {

    private final com.example.dome.persistence.TradeRepository tradeRepository;
    private final com.example.dome.cache.MarketDataCache marketDataCache;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public EventProcessor(com.example.dome.persistence.TradeRepository tradeRepository,
                          com.example.dome.cache.MarketDataCache marketDataCache,
                          org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        this.tradeRepository = tradeRepository;
        this.marketDataCache = marketDataCache;
        this.messagingTemplate = messagingTemplate;
    }

    // For buffering events if needed.
    // In low-latency systems, we might use a ring buffer (Disruptor).
    // For now, simple standard queue or direct processing.
    
    public void onTrade(TradeEvent event) {
        com.example.dome.model.Trade trade = event.trade();
        
        try {
            tradeRepository.save(trade);
            
            // Publish to WebSocket
            messagingTemplate.convertAndSend("/topic/trades/" + trade.getSymbol(), trade);
            
        } catch (Exception e) {
            System.err.println("Failed to persist/publish trade: " + e.getMessage());
        }
    }
    
    public void onBookUpdate(String symbol, java.math.BigDecimal bestBid, java.math.BigDecimal bestAsk) {
        // Simple DTO for book update
        record BookUpdate(String symbol, java.math.BigDecimal bestBid, java.math.BigDecimal bestAsk, long timestamp) {}
        
        BookUpdate update = new BookUpdate(symbol, bestBid, bestAsk, System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/book/" + symbol, update);
        
        // Also update Redis Cache
        marketDataCache.updateBestBid(symbol, bestBid);
        marketDataCache.updateBestAsk(symbol, bestAsk);
    }
}
