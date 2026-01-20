package com.example.dome.engine;

import com.example.dome.event.EventProcessor;
import com.example.dome.model.Order;
import com.example.dome.model.OrderSide;
import com.example.dome.model.OrderStatus;
import com.example.dome.model.OrderType;
import com.example.dome.persistence.RocksDBOrderDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecoveryTest {

    private RocksDB rocksDB;
    private RocksDBOrderDao orderDao;
    private File tempDbDir;
    private EngineRegistry registry;
    private EventProcessor eventProcessor;
    private com.example.dome.cache.MarketDataCache marketDataCache;

    @BeforeEach
    void setUp() throws Exception {
        RocksDB.loadLibrary();
        Path tempPath = Files.createTempDirectory("recovery_test_");
        tempDbDir = tempPath.toFile();
        
        Options options = new Options().setCreateIfMissing(true);
        rocksDB = RocksDB.open(options, tempDbDir.getAbsolutePath());
        orderDao = new RocksDBOrderDao(rocksDB);
        eventProcessor = Mockito.mock(EventProcessor.class);
        marketDataCache = Mockito.mock(com.example.dome.cache.MarketDataCache.class);
        
        registry = new EngineRegistry(eventProcessor, orderDao, marketDataCache);
    }

    @AfterEach
    void tearDown() {
        if (rocksDB != null) {
            rocksDB.close();
        }
        deleteDirectory(tempDbDir);
    }

    @Test
    void testRecovery() {
        // 1. Populate DB with orders
        // Active Order: Buy 100 @ 150
        Order activeOrder = Order.builder()
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
        orderDao.save(activeOrder);
        
        // Filled Order: Sell 50 @ 150 (Should be ignored)
        Order filledOrder = Order.builder()
                .orderId(UUID.randomUUID())
                .symbol("AAPL")
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("150.00"))
                .quantity(50)
                .filledQuantity(50)
                .status(OrderStatus.FILLED)
                .timestamp(Instant.now())
                .build();
        orderDao.save(filledOrder);
        
        // 2. Start Registry (simulating restart)
        registry.recoverState();
        
        // 3. Verify State
        MatchingEngine engine = registry.getEngine("AAPL");
        OrderBook book = engine.getOrderBook();
        assertNotNull(book);
        assertNotNull(book.getBestBid());
        assertEquals(new BigDecimal("150.00"), book.getBestBid().getPrice());
        assertEquals(100, book.getBestBid().getTotalQuantity());
        
        // Ensure filled order is NOT in book (Best Ask should be null)
        assertNull(book.getBestAsk());
    }

    private void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }
}
