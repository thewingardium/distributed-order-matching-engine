package com.example.dome.engine.disruptor;

import com.example.dome.event.EventProcessor;
import com.example.dome.event.TradeEvent;
import com.example.dome.model.Order;
import com.example.dome.model.Trade;
import com.example.dome.persistence.OrderDao;
import com.lmax.disruptor.EventHandler;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PersistenceEventHandler implements EventHandler<OrderCommand> {

    private final OrderDao orderDao;
    private final EventProcessor eventProcessor;

    @Override
    public void onEvent(OrderCommand event, long sequence, boolean endOfBatch) throws Exception {
        Order order = event.getOrder();
        if (order == null) {
            return;
        }

        // 1. Persist Modified Resting Orders
        List<Order> modifiedOrders = event.getModifiedOrders();
        if (modifiedOrders != null) {
            for (Order modifiedOrder : modifiedOrders) {
                orderDao.save(modifiedOrder);
            }
        }
        
        // 2. Persist the Incoming Order (Matched or New)
        // Note: The order state (filled quantity) was mutated by MatchingEngine.
        orderDao.save(order);
        
        // 3. Persist Trades and Publish Events
        List<Trade> trades = event.getTrades();
        if (trades != null) {
            for (Trade trade : trades) {
                 eventProcessor.onTrade(new TradeEvent(trade));
            }
        }
        
        // Complete the future for the caller (Controller)
        if (event.getResultFuture() != null) {
            event.getResultFuture().complete(trades);
        }
        
        // Clean up command for reuse? No, Disruptor reuses event object.
        event.clear();
    }
}
