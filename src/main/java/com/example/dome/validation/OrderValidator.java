package com.example.dome.validation;

import com.example.dome.model.Order;
import com.example.dome.model.OrderType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderValidator {

    public void validateOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        
        order.validate();

        // Additional business logic validation
        if (order.getType() == OrderType.LIMIT && order.getPrice() == null) {
            throw new IllegalArgumentException("Limit orders must have a price");
        }
        
        if (order.getSymbol() == null || order.getSymbol().isBlank()) {
             throw new IllegalArgumentException("Symbol cannot be empty");
        }
    }

    public boolean checkRiskLimits(Order order) {
        // Placeholder for risk checks (max order value, position limits, etc.)
        return true; 
    }
}
