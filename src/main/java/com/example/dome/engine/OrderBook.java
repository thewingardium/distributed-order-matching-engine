package com.example.dome.engine;

import com.example.dome.model.Order;
import com.example.dome.model.OrderSide;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages the Bids and Asks for a specific symbol.
 * Uses TreeMaps for maintaining price ordering.
 * Thread-safe using ReentrantReadWriteLock.
 */
public class OrderBook {

    private final String symbol;
    
    // Bids: Decreasing order (Highest buy price first)
    private final TreeMap<BigDecimal, PriceLevel> bids;
    
    // Asks: Increasing order (Lowest sell price first)
    private final TreeMap<BigDecimal, PriceLevel> asks;
    
    // Lock for structure modification (adding/removing price levels)
    // Individual PriceLevels are thread-safe for adding orders, but the Map itself needs protection.
    private final ReentrantReadWriteLock lock;

    public OrderBook(String symbol) {
        this.symbol = symbol;
        this.bids = new TreeMap<>(Comparator.reverseOrder());
        this.asks = new TreeMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    public void addOrder(Order order) {
        if (!order.getSymbol().equals(this.symbol)) {
             throw new IllegalArgumentException("Order symbol mismatch");
        }
        
        // Acquire lock to ensure PriceLevel existence or creation
        lock.readLock().lock();
        try {
            PriceLevel level = getPriceLevel(order.getSide(), order.getPrice());
            if (level != null) {
                // Determine if we can just add to existing level without write lock
                // We retrieved it, so we can add to it safely as PriceLevel is concurrent.
                level.addOrder(order);
                return;
            }
        } finally {
            lock.readLock().unlock();
        }

        // If level doesn't exist, we need write lock
        lock.writeLock().lock();
        try {
            // Double check
            PriceLevel level = getPriceLevel(order.getSide(), order.getPrice());
            if (level == null) {
                level = new PriceLevel(order.getPrice());
                if (order.getSide() == OrderSide.BUY) {
                    bids.put(order.getPrice(), level);
                } else {
                    asks.put(order.getPrice(), level);
                }
            }
            level.addOrder(order);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void cancelOrder(Order order) {
        if (!order.getSymbol().equals(this.symbol)) {
            throw new IllegalArgumentException("Order symbol mismatch");
        }
        
        lock.readLock().lock();
        try {
            PriceLevel level = getPriceLevel(order.getSide(), order.getPrice());
            if (level != null) {
                level.cancelOrder(order);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public void modifyOrder(UUID orderId, Order newOrder) {
        // Modification is usually Cancel + New Order.
        // We need the original order to cancel it. 
        // This implies we need an OrderId -> Order index.
        // We haven't built an OrderId index yet.
        // For now, we will assume the caller provides the original order or we skip this.
        // Task requirement: "Implement addOrder, cancelOrder, modifyOrder"
        // Let's add an Index: Map<UUID, Order> orderIndex.
        throw new UnsupportedOperationException("Modify requires Order Index lookup - pending implementation");
    }

    private PriceLevel getPriceLevel(OrderSide side, BigDecimal price) {
        return side == OrderSide.BUY ? bids.get(price) : asks.get(price);
    }
    
    public PriceLevel getBestBid() {
        lock.readLock().lock();
        try {
            Map.Entry<BigDecimal, PriceLevel> entry = bids.firstEntry();
            return entry == null ? null : entry.getValue();
        } finally {
            lock.readLock().unlock();
        }
    }

    public PriceLevel getBestAsk() {
        lock.readLock().lock();
        try {
            Map.Entry<BigDecimal, PriceLevel> entry = asks.firstEntry();
            return entry == null ? null : entry.getValue();
        } finally {
            lock.readLock().unlock();
        }
    }

    // Snapshot mechanism
    public Map<String, List<PriceLevelSnapshot>> getSnapshot() {
        lock.readLock().lock();
        try {
            List<PriceLevelSnapshot> bidSnap = new ArrayList<>();
            List<PriceLevelSnapshot> askSnap = new ArrayList<>();
            
            bids.values().forEach(lvl -> bidSnap.add(new PriceLevelSnapshot(lvl.getPrice(), lvl.getTotalQuantity())));
            asks.values().forEach(lvl -> askSnap.add(new PriceLevelSnapshot(lvl.getPrice(), lvl.getTotalQuantity())));
            
            Map<String, List<PriceLevelSnapshot>> snapshot = new HashMap<>();
            snapshot.put("bids", bidSnap);
            snapshot.put("asks", askSnap);
            return snapshot;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // Simple record for snapshot
    public record PriceLevelSnapshot(BigDecimal price, long quantity) {}
}
