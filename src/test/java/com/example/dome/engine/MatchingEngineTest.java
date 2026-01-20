package com.example.dome.engine;

import com.example.dome.model.Order;
import com.example.dome.model.OrderSide;
import com.example.dome.model.OrderStatus;
import com.example.dome.model.OrderType;
import com.example.dome.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

import com.example.dome.event.EventProcessor;
import com.example.dome.persistence.OrderDao;
import org.mockito.Mockito;

class MatchingEngineTest {

    private MatchingEngine engine;
    private OrderDao orderDao;
    private EventProcessor eventProcessor;
    private com.example.dome.cache.MarketDataCache marketDataCache;

    @BeforeEach
    void setUp() {
        orderDao = Mockito.mock(OrderDao.class);
        eventProcessor = Mockito.mock(EventProcessor.class);
        marketDataCache = Mockito.mock(com.example.dome.cache.MarketDataCache.class);
        engine = new MatchingEngine("AAPL", eventProcessor, orderDao);
    }

    @Test
    void testSimpleLimitMatch() {
        // Resting Sell Order: 100 @ 150.00
        Order sellOrder = createOrder(OrderSide.SELL, new BigDecimal("150.00"), 100);
        engine.processOrder(sellOrder);

        // Aggressive Buy Order: 50 @ 150.00
        Order buyOrder = createOrder(OrderSide.BUY, new BigDecimal("150.00"), 50);
        List<Trade> trades = engine.processOrder(buyOrder);

        assertEquals(1, trades.size());
        Trade trade = trades.get(0);
        assertEquals(50, trade.getQuantity());
        assertEquals(new BigDecimal("150.00"), trade.getPrice());
        
        // Check remaining quantity
        assertEquals(50, sellOrder.getRemainingQuantity());
        assertEquals(OrderStatus.PARTIALLY_FILLED, sellOrder.getStatus());
        assertEquals(0, buyOrder.getRemainingQuantity());
        assertEquals(OrderStatus.FILLED, buyOrder.getStatus());
    }

    @Test
    void testMarketOrderSweep() {
        // Resting Sells: 10 @ 100, 20 @ 101
        Order s1 = createOrder(OrderSide.SELL, new BigDecimal("100.00"), 10);
        Order s2 = createOrder(OrderSide.SELL, new BigDecimal("101.00"), 20);
        engine.processOrder(s1);
        engine.processOrder(s2);

        // Aggressive Market Buy: 25. Should fill 10 @ 100 and 15 @ 101.
        Order marketBuy = Order.builder()
                .orderId(UUID.randomUUID())
                .symbol("AAPL")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .price(null) // Market order
                .quantity(25)
                .status(OrderStatus.NEW)
                .timestamp(Instant.now())
                .build();

        List<Trade> trades = engine.processOrder(marketBuy);

        assertEquals(2, trades.size());
        
        Trade t1 = trades.get(0);
        assertEquals(10, t1.getQuantity());
        assertEquals(new BigDecimal("100.00"), t1.getPrice());

        Trade t2 = trades.get(1);
        assertEquals(15, t2.getQuantity());
        assertEquals(new BigDecimal("101.00"), t2.getPrice());

        assertEquals(0, marketBuy.getRemainingQuantity());
        assertEquals(OrderStatus.FILLED, marketBuy.getStatus());
        
        // Verify OrderBook state
        OrderBook book = engine.getOrderBook();
        // 100 level should be REMOVED if empty. Next best is 101.
        assertNotNull(book.getBestAsk());
        assertEquals(new BigDecimal("101.00"), book.getBestAsk().getPrice());
        assertEquals(5, book.getBestAsk().getTotalQuantity());
    }

    @Test
    void testMatchWithCancelledOrders() {
        // Levels: 100.00
        // Order 1: Sell 10 (CANCELLED)
        // Order 2: Sell 20 (ACTIVE)
        
        Order s1 = createOrder(OrderSide.SELL, new BigDecimal("100.00"), 10);
        engine.processOrder(s1);
        
        Order s2 = createOrder(OrderSide.SELL, new BigDecimal("100.00"), 20);
        engine.processOrder(s2);
        
        // Cancel s1
         // Mocking findById is needed because MatchingEngine.cancelOrder calls orderDao.findById
        Mockito.when(orderDao.findById(s1.getOrderId().toString())).thenReturn(s1);

        engine.cancelOrder(s1.getOrderId());
        
        // Buy 15. Should skip s1 and match 15 from s2.
        Order buy = createOrder(OrderSide.BUY, new BigDecimal("100.00"), 15);
        List<Trade> trades = engine.processOrder(buy);
        
        assertEquals(1, trades.size());
        assertEquals(15, trades.get(0).getQuantity());
        assertEquals(s2.getOrderId(), trades.get(0).getSellOrderId());
        
        // s2 remaining: 5
        assertEquals(5, s2.getRemainingQuantity());
    }
    
    @Test
    void testNoMatch() {
        // Sell @ 150
        Order s1 = createOrder(OrderSide.SELL, new BigDecimal("150.00"), 10);
        engine.processOrder(s1);
        
        // Buy @ 149
        Order b1 = createOrder(OrderSide.BUY, new BigDecimal("149.00"), 10);
        List<Trade> trades = engine.processOrder(b1);
        
        assertTrue(trades.isEmpty());
        // Both match in book
        OrderBook book = engine.getOrderBook();
        assertEquals(10, book.getBestAsk().getTotalQuantity());
        assertEquals(10, book.getBestBid().getTotalQuantity());
    }

    private Order createOrder(OrderSide side, BigDecimal price, long qty) {
        return Order.builder()
                .orderId(UUID.randomUUID())
                .symbol("AAPL")
                .side(side)
                .type(OrderType.LIMIT)
                .price(price)
                .quantity(qty)
                .status(OrderStatus.NEW)
                .timestamp(Instant.now())
                .build();
    }
}
