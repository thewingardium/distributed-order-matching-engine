package com.example.dome.controller;

import com.example.dome.dto.OrderBookDTO;
import com.example.dome.dto.OrderRequest;
import com.example.dome.dto.OrderResponse;
import com.example.dome.engine.MatchingEngine;
import com.example.dome.engine.OrderBook;
import com.example.dome.engine.OrderBook.PriceLevelSnapshot;
import com.example.dome.model.Order;
import com.example.dome.model.OrderStatus;
import com.example.dome.model.Trade;
import com.example.dome.persistence.TradeRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final com.example.dome.engine.EngineRegistry engineRegistry;
    private final TradeRepository tradeRepository;
    private final com.lmax.disruptor.dsl.Disruptor<com.example.dome.engine.disruptor.OrderCommand> disruptor;

    public OrderController(com.example.dome.engine.EngineRegistry engineRegistry, 
                           TradeRepository tradeRepository,
                           com.lmax.disruptor.dsl.Disruptor<com.example.dome.engine.disruptor.OrderCommand> disruptor) {
        this.engineRegistry = engineRegistry;
        this.tradeRepository = tradeRepository;
        this.disruptor = disruptor;
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        // Validation check (Symbol existence)
        if (engineRegistry.getEngine(request.getSymbol()) == null) {
            return ResponseEntity.badRequest().body(OrderResponse.builder()
                    .message("Unknown symbol: " + request.getSymbol())
                    .status(OrderStatus.REJECTED)
                    .timestamp(Instant.now())
                    .build());
        }

        Order order = Order.builder()
                .orderId(UUID.randomUUID())
                .symbol(request.getSymbol())
                .side(request.getSide())
                .type(request.getType())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .status(OrderStatus.NEW)
                .timestamp(Instant.now())
                .build();

        // Validate logic
        try {
            order.validate();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(OrderResponse.builder()
                    .message(e.getMessage())
                    .status(OrderStatus.REJECTED)
                    .timestamp(Instant.now())
                    .build());
        }

        // Process via Disruptor
        java.util.concurrent.CompletableFuture<List<Trade>> future = new java.util.concurrent.CompletableFuture<>();
        
        disruptor.publishEvent((event, sequence) -> {
            event.setOrder(order);
            event.setResultFuture(future);
        });

        List<Trade> trades;
        try {
            // Wait for pipeline to complete (Match -> Persist)
            trades = future.get();
        } catch (Exception e) {
             return ResponseEntity.internalServerError().body(OrderResponse.builder()
                    .message("Processing failed: " + e.getMessage())
                    .status(OrderStatus.REJECTED)
                    .timestamp(Instant.now())
                    .build());
        }

        return ResponseEntity.ok(OrderResponse.builder()
                .orderId(order.getOrderId())
                .status(order.getStatus())
                .message("Order processed. Trades: " + (trades != null ? trades.size() : 0))
                .timestamp(Instant.now())
                .build());
    }

    @DeleteMapping("/orders/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID id) {
        try {
            // Registry handles lookup and routing
            engineRegistry.cancelOrder(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/orderbook/{symbol}")
    public ResponseEntity<OrderBookDTO> getOrderBook(@PathVariable String symbol) {
        MatchingEngine engine = engineRegistry.getEngine(symbol);
        OrderBook book = engine.getOrderBook();
        if (book == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, List<PriceLevelSnapshot>> snapshot = book.getSnapshot();
        
        List<OrderBookDTO.LevelDTO> bids = snapshot.get("bids").stream()
                .map(s -> OrderBookDTO.LevelDTO.builder()
                        .price(s.price())
                        .quantity(s.quantity())
                        .build())
                .collect(Collectors.toList());

        List<OrderBookDTO.LevelDTO> asks = snapshot.get("asks").stream()
                .map(s -> OrderBookDTO.LevelDTO.builder()
                        .price(s.price())
                        .quantity(s.quantity())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(OrderBookDTO.builder()
                .symbol(symbol)
                .bids(bids)
                .asks(asks)
                .timestamp(System.currentTimeMillis())
                .build());
    }

    @GetMapping("/trades")
    public ResponseEntity<List<Trade>> getTrades() {
        return ResponseEntity.ok(tradeRepository.findAll());
    }
}
