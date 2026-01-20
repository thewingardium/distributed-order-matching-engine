package com.example.dome.persistence;

import com.example.dome.model.Order;

import java.util.List;

public interface OrderDao {
    void save(Order order);
    void delete(String orderId);
    Order findById(String orderId);
    List<Order> findAll();
}
