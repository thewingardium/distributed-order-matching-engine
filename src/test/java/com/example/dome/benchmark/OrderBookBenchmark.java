package com.example.dome.benchmark;

import com.example.dome.engine.OrderBook;
import com.example.dome.model.Order;
import com.example.dome.model.OrderSide;
import com.example.dome.model.OrderStatus;
import com.example.dome.model.OrderType;
import org.openjdk.jmh.annotations.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Threads(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class OrderBookBenchmark {

    private OrderBook orderBook;
    private Order buyOrder;

    @Setup
    public void setup() {
        orderBook = new OrderBook("AAPL");
        buyOrder = Order.builder()
                .orderId(UUID.randomUUID())
                .symbol("AAPL")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("150.00"))
                .quantity(100)
                .filledQuantity(0)
                .status(OrderStatus.NEW)
                .timestamp(Instant.now())
                .build();
    }

    @Benchmark
    public void addOrder() {
        // Note: Thread safety might require new order IDs or cleared book.
        // For simple throughput, we add same object (if key allows) or create new.
        // OrderBook uses Map. Logic might replace.
        // To verify add speed, we might want unique orders.
        // But UUID creation is slow.
        // Let's just add the same order. It will go to the same PriceLevel queue.
        orderBook.addOrder(buyOrder);
    }
}
