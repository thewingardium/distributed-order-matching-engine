package com.example.dome.engine;

import com.example.dome.model.Order;
import com.example.dome.model.OrderSide;
import com.example.dome.model.OrderStatus;
import com.example.dome.model.OrderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class PriceLevelTest {

    @Test
    void testSingleThreadAddAndPoll() {
        BigDecimal price = new BigDecimal("100.00");
        PriceLevel level = new PriceLevel(price);

        Order order1 = createOrder(price, 10);
        Order order2 = createOrder(price, 20);

        level.addOrder(order1);
        level.addOrder(order2);

        assertEquals(30, level.getTotalQuantity());

        Order polled1 = level.poll();
        assertEquals(order1, polled1);
        assertEquals(20, level.getTotalQuantity());

        Order polled2 = level.poll();
        assertEquals(order2, polled2);
        assertEquals(0, level.getTotalQuantity());
        assertTrue(level.isEmpty());
    }

    @Test
    void testConcurrentAdd() throws InterruptedException {
        BigDecimal price = new BigDecimal("100.00");
        PriceLevel level = new PriceLevel(price);
        int threadCount = 10;
        int ordersPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ordersPerThread; j++) {
                        level.addOrder(createOrder(price, 1)); // 1 qty each
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals((long) threadCount * ordersPerThread, level.getTotalQuantity());
        executor.shutdown();
    }

    private Order createOrder(BigDecimal price, long qty) {
        return Order.builder()
                .orderId(UUID.randomUUID())
                .symbol("TEST")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(price)
                .quantity(qty)
                .status(OrderStatus.NEW)
                .timestamp(Instant.now())
                .build();
    }
}
