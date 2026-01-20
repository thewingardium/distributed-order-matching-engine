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

    // Index for O(1) lookup of orders by ID
    private final Map<UUID, Order> orderIndex = new HashMap<>();

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
        lock.writeLock().lock(); // WRITE lock needed for Index update
        try {
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
            orderIndex.put(order.getOrderId(), order);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void cancelOrder(Order order) {
        if (!order.getSymbol().equals(this.symbol)) {
            throw new IllegalArgumentException("Order symbol mismatch");
        }
        
        lock.writeLock().lock(); // WRITE lock for Index removal
        try {
            PriceLevel level = getPriceLevel(order.getSide(), order.getPrice());
            if (level != null) {
                level.cancelOrder(order);
            }
            orderIndex.remove(order.getOrderId());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // Package-private or public accessor if needed
    public Order getOrder(UUID orderId) {
        lock.readLock().lock();
        try {
            return orderIndex.get(orderId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void modifyOrder(UUID orderId, Order newOrder) {
        lock.writeLock().lock();
        try {
            Order oldOrder = orderIndex.get(orderId);
            if (oldOrder == null) {
                throw new IllegalArgumentException("Order not found: " + orderId);
            }
            
            // simple Cancel/Replace (Loss order priority)
            cancelOrder(oldOrder);
            addOrder(newOrder);
            
            // TODO: In future, if optimization needed (e.g. reduce size keeps priority),
            // handle here by checking if price is same and size < oldSize.
        } finally {
            lock.writeLock().unlock();
        }
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
    
    public void removeLevel(BigDecimal price) {
        lock.writeLock().lock();
        try {
            if (bids.containsKey(price)) {
                bids.remove(price);
            } else if (asks.containsKey(price)) {
                asks.remove(price);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // Simple record for snapshot
    public record PriceLevelSnapshot(BigDecimal price, long quantity) {}
}
