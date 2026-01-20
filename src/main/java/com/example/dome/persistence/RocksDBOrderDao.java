package com.example.dome.persistence;

import com.example.dome.model.Order;
import com.example.dome.model.OrderSide;
import com.example.dome.model.OrderStatus;
import com.example.dome.model.OrderType;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class RocksDBOrderDao implements OrderDao {

    private final RocksDB rocksDB;

    public RocksDBOrderDao(RocksDB rocksDB) {
        this.rocksDB = rocksDB;
    }

    @Override
    public void save(Order order) {
        try {
            rocksDB.put(order.getOrderId().toString().getBytes(), serialize(order));
        } catch (RocksDBException | IOException e) {
            throw new RuntimeException("Error saving order to RocksDB", e);
        }
    }

    @Override
    public void delete(String orderId) {
        try {
            rocksDB.delete(orderId.getBytes());
        } catch (RocksDBException e) {
            throw new RuntimeException("Error deleting order from RocksDB", e);
        }
    }

    @Override
    public Order findById(String orderId) {
        try {
            byte[] bytes = rocksDB.get(orderId.getBytes());
            if (bytes == null) return null;
            return deserialize(bytes);
        } catch (RocksDBException | IOException e) {
            throw new RuntimeException("Error finding order in RocksDB", e);
        }
    }

    @Override
    public List<Order> findAll() {
        List<Order> orders = new ArrayList<>();
        try (RocksIterator iterator = rocksDB.newIterator()) {
            iterator.seekToFirst();
            while (iterator.isValid()) {
                orders.add(deserialize(iterator.value()));
                iterator.next();
            }
        } catch (IOException e) {
             throw new RuntimeException("Error reading orders from RocksDB", e);
        }
        return orders;
    }

    private byte[] serialize(Order order) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            
            dos.writeUTF(order.getOrderId().toString());
            dos.writeUTF(order.getSymbol());
            dos.writeUTF(order.getSide().name());
            dos.writeUTF(order.getType().name());
            
            // Price can be null for Market orders
            if (order.getPrice() != null) {
                dos.writeBoolean(true);
                dos.writeUTF(order.getPrice().toString());
            } else {
                dos.writeBoolean(false);
            }
            
            dos.writeLong(order.getQuantity());
            dos.writeLong(order.getFilledQuantity());
            dos.writeUTF(order.getStatus().name());
            dos.writeLong(order.getTimestamp().toEpochMilli());
            
            return baos.toByteArray();
        }
    }

    private Order deserialize(byte[] bytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bais)) {
            
            UUID orderId = UUID.fromString(dis.readUTF());
            String symbol = dis.readUTF();
            OrderSide side = OrderSide.valueOf(dis.readUTF());
            OrderType type = OrderType.valueOf(dis.readUTF());
            
            BigDecimal price = null;
            if (dis.readBoolean()) {
                price = new BigDecimal(dis.readUTF());
            }
            
            long quantity = dis.readLong();
            long filledQuantity = dis.readLong();
            OrderStatus status = OrderStatus.valueOf(dis.readUTF());
            Instant timestamp = Instant.ofEpochMilli(dis.readLong());
            
            return Order.builder()
                    .orderId(orderId)
                    .symbol(symbol)
                    .side(side)
                    .type(type)
                    .price(price)
                    .quantity(quantity)
                    .filledQuantity(filledQuantity)
                    .status(status)
                    .timestamp(timestamp)
                    .build();
        }
    }
}
