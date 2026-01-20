package com.example.dome.engine;

import com.example.dome.model.Order;
import org.jctools.queues.MpscLinkedQueue;

import java.math.BigDecimal;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a specific price level in the order book.
 * Contains a queue of orders at this price and the total quantity.
 * Uses JCTools MpscLinkedQueue for lock-free high-performance.
 */
public class PriceLevel {

    private final BigDecimal price;
    private final Queue<Order> orders;
    private final AtomicLong totalQuantity;

    public PriceLevel(BigDecimal price) {
        this.price = price;
        // Multi-Producer Single-Consumer queue.
        // Producers: multiple threads adding orders.
        // Consumer: Single matching engine thread consuming orders.
        this.orders = new MpscLinkedQueue<>();
        this.totalQuantity = new AtomicLong(0);
    }

    public void addOrder(Order order) {
        if (order.getPrice().compareTo(this.price) != 0) {
            throw new IllegalArgumentException("Order price does not match level price");
        }
        this.orders.add(order);
        this.totalQuantity.addAndGet(order.getRemainingQuantity());
    }

    /**
     * logically cancels the order.
     * Note: MPSC queue contains the order still. Matching engine must skip CANCELED orders.
     * We decrement the volume immediately to reflect true book depth.
     */
    public void cancelOrder(Order order) {
        if (order.getStatus() != com.example.dome.model.OrderStatus.CANCELED) {
             long qty = order.getRemainingQuantity();
             if (qty > 0) {
                 this.totalQuantity.addAndGet(-qty);
             }
             order.setStatus(com.example.dome.model.OrderStatus.CANCELED);
        }
    }

    public Order peek() {
        return this.orders.peek();
    }

    public void reduceTotalQuantity(long quantity) {
        if (quantity > 0) {
            this.totalQuantity.addAndGet(-quantity);
        }
    }

    public Order poll() {
        Order order = this.orders.poll();
        if (order != null && order.getStatus() != com.example.dome.model.OrderStatus.CANCELED) {
            // Only reduce quantity if NOT cancelled (cancelled orders already reduced totalQuantity)
            // And if we rely on external reduction for Fills, we must be careful.
            // If we reduce via reduceTotalQuantity during match, remainingQty decreases simultaneously.
            // So if remainingQty is 0, we subtract 0. Correct.
            // If remainingQty > 0 (polled prematurely?), we subtract remainder. Correct.
            this.totalQuantity.addAndGet(-order.getRemainingQuantity());
        }
        return order;
    }

    public boolean isEmpty() {
        return this.orders.isEmpty();
    }
    
    public BigDecimal getPrice() {
        return price;
    }

    public long getTotalQuantity() {
        return totalQuantity.get();
    }
}
