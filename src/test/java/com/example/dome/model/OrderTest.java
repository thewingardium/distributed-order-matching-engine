package com.example.dome.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void testOrderBuilderAndDefaults() {
        Order order = Order.builder()
                .orderId(UUID.randomUUID())
                .symbol("AAPL")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("150.00"))
                .quantity(100)
                .status(OrderStatus.NEW)
                .timestamp(Instant.now())
                .build();

        assertNotNull(order.getOrderId());
        assertEquals(0, order.getFilledQuantity());
        assertEquals(100, order.getRemainingQuantity());
        assertEquals(OrderStatus.NEW, order.getStatus());
    }

    @Test
    void testValidatePositiveQuantity() {
        Order order = Order.builder()
                .orderId(UUID.randomUUID())
                .symbol("AAPL")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .price(BigDecimal.ZERO)
                .quantity(0) // Invalid
                .status(OrderStatus.NEW)
                .timestamp(Instant.now())
                .build();

        assertThrows(IllegalArgumentException.class, order::validate);
    }

    @Test
    void testValidatePriceForLimitOrder() {
        Order order = Order.builder()
                .orderId(UUID.randomUUID())
                .symbol("AAPL")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(BigDecimal.ZERO) // Invalid for Limit
                .quantity(100)
                .status(OrderStatus.NEW)
                .timestamp(Instant.now())
                .build();

        assertThrows(IllegalArgumentException.class, order::validate);
    }

    @Test
    void testFillMethods() {
        Order order = Order.builder()
                .orderId(UUID.randomUUID())
                .symbol("AAPL")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("150.00"))
                .quantity(100)
                .status(OrderStatus.NEW)
                .timestamp(Instant.now())
                .build();

        // Partial fill
        order.fill(50);
        assertEquals(50, order.getFilledQuantity());
        assertEquals(50, order.getRemainingQuantity());
        assertEquals(OrderStatus.PARTIALLY_FILLED, order.getStatus());

        // Complete fill
        order.fill(50);
        assertEquals(100, order.getFilledQuantity());
        assertEquals(0, order.getRemainingQuantity());
        assertEquals(OrderStatus.FILLED, order.getStatus());
    }

    @Test
    void testOverfillThrowsException() {
        Order order = Order.builder()
                .orderId(UUID.randomUUID())
                .symbol("AAPL")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("150.00"))
                .quantity(100)
                .status(OrderStatus.NEW)
                .timestamp(Instant.now())
                .build();

        assertThrows(IllegalArgumentException.class, () -> order.fill(101));
    }
}
