package com.example.dome.integration;

import com.example.dome.model.Trade;
import com.example.dome.model.Order;
import com.example.dome.model.OrderSide;
import com.example.dome.model.OrderType;
import com.example.dome.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "rocksdb.path=data/rocksdb/test_ws")
public class WebSocketTest {

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;
    private StompSession session;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.dome.event.EventProcessor eventProcessor;

    @BeforeEach
    public void setup() {
        stompClient = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.getObjectMapper().findAndRegisterModules();
        stompClient.setMessageConverter(converter);
    }

    @Test
    public void verifyTradeUpdates() throws Exception {
        String url = "ws://localhost:" + port + "/ws";
        session = stompClient.connect(url, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);

        BlockingQueue<Trade> blockingQueue = new LinkedBlockingQueue<>();

        session.subscribe("/topic/trades/AAPL", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Trade.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                blockingQueue.offer((Trade) payload);
            }
        });
        
        // Wait for subscription to establish
        Thread.sleep(1000);

        // Simulate a trade
        Trade trade = Trade.builder()
                .tradeId(UUID.randomUUID())
                .buyOrderId(UUID.randomUUID())
                .sellOrderId(UUID.randomUUID())
                .symbol("AAPL")
                .price(new BigDecimal("150.00"))
                .quantity(100)
                .timestamp(Instant.now())
                .build();
        
        // We use eventProcessor.onTrade which publishes to WS
        // Note: onTrade creates a new TradeEvent wrapper
        eventProcessor.onTrade(new com.example.dome.event.TradeEvent(trade));

        Trade received = blockingQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull(received, "Should receive a trade via WebSocket");
        assertEquals(trade.getTradeId(), received.getTradeId());
    }
}
