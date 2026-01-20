package com.example.dome.engine.disruptor;

import com.example.dome.engine.MatchingEngine;
import com.example.dome.model.Trade;
import com.lmax.disruptor.EventHandler;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class MatchingEventHandler implements EventHandler<OrderCommand> {

    private final com.example.dome.engine.EngineRegistry engineRegistry;

    @Override
    public void onEvent(OrderCommand event, long sequence, boolean endOfBatch) throws Exception {
        if (event.getOrder() == null) {
            return;
        }
        
        com.example.dome.engine.MatchingEngine engine = engineRegistry.getEngine(event.getOrder().getSymbol());
        // If engine doesn't exist? It should be created or validation happens before.
        // Assuming validation happens at Controller level.
        
        com.example.dome.engine.MatchResult result = engine.match(event.getOrder());
        event.setTrades(result.trades());
        event.setModifiedOrders(result.modifiedOrders());
    }
}
