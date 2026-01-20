package com.example.dome.persistence;

import com.example.dome.model.Order;
import com.example.dome.model.OrderSide;
import com.example.dome.model.OrderStatus;
import com.example.dome.model.OrderType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RocksDBOrderDaoTest {

    private RocksDB rocksDB;
    private RocksDBOrderDao orderDao;
    private File tempDbDir;

    @BeforeEach
    void setUp() throws IOException, RocksDBException {
        RocksDB.loadLibrary();
        // Create temp directory for each test
        Path tempPath = Files.createTempDirectory("rocksdb_test_");
        tempDbDir = tempPath.toFile();
        
        Options options = new Options().setCreateIfMissing(true);
        rocksDB = RocksDB.open(options, tempDbDir.getAbsolutePath());
        orderDao = new RocksDBOrderDao(rocksDB);
    }

    @AfterEach
    void tearDown() {
        if (rocksDB != null) {
            rocksDB.close();
        }
        // Cleanup temp dir
        deleteDirectory(tempDbDir);
    }

    @Test
    void testSaveAndFind() {
        Order order = Order.builder()
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

        orderDao.save(order);

        Order retrieved = orderDao.findById(order.getOrderId().toString());
        assertNotNull(retrieved);
        assertEquals(order.getOrderId(), retrieved.getOrderId());
        assertEquals(order.getPrice(), retrieved.getPrice());
        assertEquals(order.getStatus(), retrieved.getStatus());
        assertEquals(order.getTimestamp().toEpochMilli(), retrieved.getTimestamp().toEpochMilli());
    }

    @Test
    void testFindAll() {
        Order o1 = createOrder("AAPL", 100);
        Order o2 = createOrder("GOOG", 200);

        orderDao.save(o1);
        orderDao.save(o2);

        List<Order> all = orderDao.findAll();
        assertEquals(2, all.size());
        
        // Sort by symbol to ensure deterministic assertion
        all.sort(Comparator.comparing(Order::getSymbol));
        assertEquals("AAPL", all.get(0).getSymbol());
        assertEquals("GOOG", all.get(1).getSymbol());
    }

    @Test
    void testDelete() {
        Order o1 = createOrder("AAPL", 100);
        orderDao.save(o1);
        
        assertNotNull(orderDao.findById(o1.getOrderId().toString()));
        
        orderDao.delete(o1.getOrderId().toString());
        assertNull(orderDao.findById(o1.getOrderId().toString()));
    }
    
    @Test
    void testSaveMarketOrderWithNullPrice() {
        Order market = Order.builder()
                .orderId(UUID.randomUUID())
                .symbol("MSFT")
                .side(OrderSide.SELL)
                .type(OrderType.MARKET)
                .price(null) // Null price
                .quantity(50)
                .filledQuantity(0)
                .status(OrderStatus.NEW)
                .timestamp(Instant.now())
                .build();
                
        orderDao.save(market);
        
        Order retrieved = orderDao.findById(market.getOrderId().toString());
        assertNotNull(retrieved);
        assertNull(retrieved.getPrice());
        assertEquals(OrderType.MARKET, retrieved.getType());
    }

    private Order createOrder(String symbol, long qty) {
         return Order.builder()
                .orderId(UUID.randomUUID())
                .symbol(symbol)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("100.00"))
                .quantity(qty)
                .filledQuantity(0)
                .status(OrderStatus.NEW)
                .timestamp(Instant.now())
                .build();
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
