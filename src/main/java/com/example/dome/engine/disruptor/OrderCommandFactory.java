package com.example.dome.engine.disruptor;

import com.lmax.disruptor.EventFactory;

public class OrderCommandFactory implements EventFactory<OrderCommand> {
    @Override
    public OrderCommand newInstance() {
        return new OrderCommand();
    }
}
