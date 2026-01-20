package com.example.dome.engine;

import com.example.dome.model.Order;
import com.example.dome.model.OrderSide;
import com.example.dome.model.OrderStatus;
import com.example.dome.model.OrderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookTest {

    @Test
    void testAddBidAndAskOrdering() {
        OrderBook book = new OrderBook("AAPL");
        
        // Add Bids: 100, 101, 99. Best Bid should be 101.
        book.addOrder(createOrder("AAPL", OrderSide.BUY, new BigDecimal("100.00")));
        book.addOrder(createOrder("AAPL", OrderSide.BUY, new BigDecimal("101.00")));
        book.addOrder(createOrder("AAPL", OrderSide.BUY, new BigDecimal("99.00")));
        
        assertEquals(new BigDecimal("101.00"), book.getBestBid().getPrice());
        
        // Add Asks: 102, 103, 101.5. Best Ask should be 101.5.
        book.addOrder(createOrder("AAPL", OrderSide.SELL, new BigDecimal("102.00")));
        book.addOrder(createOrder("AAPL", OrderSide.SELL, new BigDecimal("103.00")));
        book.addOrder(createOrder("AAPL", OrderSide.SELL, new BigDecimal("101.50")));
        
        assertEquals(new BigDecimal("101.50"), book.getBestAsk().getPrice());
    }

    @Test
    void testSnapshotStructure() {
        OrderBook book = new OrderBook("MSFT");
        book.addOrder(createOrder("MSFT", OrderSide.BUY, new BigDecimal("250.00"), 100));
        book.addOrder(createOrder("MSFT", OrderSide.BUY, new BigDecimal("250.00"), 50));
        book.addOrder(createOrder("MSFT", OrderSide.SELL, new BigDecimal("255.00"), 200));

        Map<String, List<OrderBook.PriceLevelSnapshot>> snapshot = book.getSnapshot();
        
        assertNotNull(snapshot.get("bids"));
        assertNotNull(snapshot.get("asks"));
        
        List<OrderBook.PriceLevelSnapshot> bids = snapshot.get("bids");
        assertEquals(1, bids.size());
        assertEquals(new BigDecimal("250.00"), bids.get(0).price());
        assertEquals(150, bids.get(0).quantity()); // 100 + 50

        List<OrderBook.PriceLevelSnapshot> asks = snapshot.get("asks");
        assertEquals(1, asks.size());
        assertEquals(200, asks.get(0).quantity());
    }

    @Test
    void testCancelOrder() {
        OrderBook book = new OrderBook("GOOG");
        Order order = createOrder("GOOG", OrderSide.BUY, new BigDecimal("1000.00"), 10);
        
        book.addOrder(order);
        assertEquals(10, book.getBestBid().getTotalQuantity());
        
        book.cancelOrder(order);
        
        // Quantity should be reduced immediately
        assertEquals(0, book.getBestBid().getTotalQuantity());
        assertEquals(OrderStatus.CANCELED, order.getStatus());
        
        // Order should be removed from index
        assertNull(book.getOrder(order.getOrderId()));
    }
    
    @Test
    void testModifyOrder() {
        OrderBook book = new OrderBook("TSLA");
        Order original = createOrder("TSLA", OrderSide.BUY, new BigDecimal("200.00"), 50);
        
        book.addOrder(original);
        assertEquals(50, book.getBestBid().getTotalQuantity());
        assertNotNull(book.getOrder(original.getOrderId()));
        
        // Modify: Reduce size to 20
        Order newOrder = createOrder("TSLA", OrderSide.BUY, new BigDecimal("200.00"), 20);
        book.modifyOrder(original.getOrderId(), newOrder);
        
        // Verify old is removed/canceled
        assertNull(book.getOrder(original.getOrderId()));
        assertEquals(OrderStatus.CANCELED, original.getStatus());
        
        // Verify new is added
        assertNotNull(book.getOrder(newOrder.getOrderId()));
        assertEquals(20, book.getBestBid().getTotalQuantity());
    }

    private Order createOrder(String symbol, OrderSide side, BigDecimal price) {
        return createOrder(symbol, side, price, 10); // default qty
    }

    private Order createOrder(String symbol, OrderSide side, BigDecimal price, long qty) {
        return Order.builder()
                .orderId(UUID.randomUUID())
                .symbol(symbol)
                .side(side)
                .type(OrderType.LIMIT)
                .price(price)
                .quantity(qty)
                .status(OrderStatus.NEW)
                .timestamp(Instant.now())
                .build();
    }
}
