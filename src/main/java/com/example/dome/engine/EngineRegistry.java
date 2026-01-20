package com.example.dome.engine;

import com.example.dome.cache.MarketDataCache;
import com.example.dome.event.EventProcessor;
import com.example.dome.model.Order;
import com.example.dome.persistence.OrderDao;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EngineRegistry {

    private final Map<String, MatchingEngine> engines = new ConcurrentHashMap<>();
    private final EventProcessor eventProcessor;
    private final OrderDao orderDao;
    private final MarketDataCache marketDataCache;

    public EngineRegistry(EventProcessor eventProcessor, OrderDao orderDao, MarketDataCache marketDataCache) {
        this.eventProcessor = eventProcessor;
        this.orderDao = orderDao;
        this.marketDataCache = marketDataCache;
    }

    public void cancelOrder(java.util.UUID orderId) {
        Order order = orderDao.findById(orderId.toString());
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        MatchingEngine engine = getEngine(order.getSymbol());
        engine.cancelOrder(orderId);
    }

    @PostConstruct
    public void recoverState() {
        System.out.println("Recovering Matching Engine State via Registry...");
        List<Order> orders = orderDao.findAll();
        int loadedCount = 0;
        
        for (Order order : orders) {
            // Only recover active orders
            if (order.getStatus() == com.example.dome.model.OrderStatus.FILLED || 
                order.getStatus() == com.example.dome.model.OrderStatus.CANCELED ||
                order.getStatus() == com.example.dome.model.OrderStatus.REJECTED) {
                continue;
            }
            
            MatchingEngine engine = getEngine(order.getSymbol());
            // Directly add to book without triggering matching or persistence
            // Since we are creating logic in MatchingEngine, we might need a specific 'recoverOrder' method
            // or we use the fact that these orders are already in DB.
            // But MatchingEngine.processOrder persists and matches. We don't want that for recovery.
            // MatchingEngine needs a way to "load" an order.
            engine.getOrderBook().addOrder(order);
            loadedCount++;
        }
        System.out.println("Recovery Complete. Loaded " + loadedCount + " active orders.");
    }

    public MatchingEngine getEngine(String symbol) {
        return engines.computeIfAbsent(symbol, s -> new MatchingEngine(s, eventProcessor, orderDao));
    }
}
