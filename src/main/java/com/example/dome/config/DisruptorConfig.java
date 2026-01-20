package com.example.dome.config;

import com.example.dome.engine.disruptor.OrderCommand;
import com.example.dome.engine.disruptor.OrderCommandFactory;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadFactory;

@Configuration
public class DisruptorConfig {

    @Bean
    public Disruptor<OrderCommand> disruptor(com.example.dome.engine.EngineRegistry engineRegistry,
                                           com.example.dome.persistence.OrderDao orderDao,
                                           com.example.dome.event.EventProcessor eventProcessor) {
        ThreadFactory threadFactory = DaemonThreadFactory.INSTANCE;
        int bufferSize = 1024; // Must be power of 2

        Disruptor<OrderCommand> disruptor = new Disruptor<>(
                new OrderCommandFactory(), 
                bufferSize, 
                threadFactory);
        
        // Define Handlers
        com.example.dome.engine.disruptor.MatchingEventHandler matchingHandler = 
            new com.example.dome.engine.disruptor.MatchingEventHandler(engineRegistry);
            
        com.example.dome.engine.disruptor.PersistenceEventHandler persistenceHandler = 
            new com.example.dome.engine.disruptor.PersistenceEventHandler(orderDao, eventProcessor);
            
        // Wire pipeline: Matching -> Persistence
        disruptor.handleEventsWith(matchingHandler).then(persistenceHandler);
        
        // Start the disruptor
        disruptor.start();
        
        return disruptor;
    }
}
