package com.example.dome.engine.disruptor;

import com.example.dome.model.Order;
import com.example.dome.model.Trade;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrderCommand {
    private Order order;
    private List<Trade> trades = new ArrayList<>();
    private List<Order> modifiedOrders = new ArrayList<>();
    
    private java.util.concurrent.CompletableFuture<List<Trade>> resultFuture;
    
    // Command Type? For now, just processing orders.
    // Could add CANCEL command here later.
    
    public void clear() {
        this.order = null;
        this.trades.clear();
        this.modifiedOrders.clear();
        this.resultFuture = null;
    }
}
