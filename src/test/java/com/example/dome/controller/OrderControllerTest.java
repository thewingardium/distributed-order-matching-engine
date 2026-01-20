package com.example.dome.controller;

import com.example.dome.dto.OrderRequest;
import com.example.dome.engine.MatchingEngine;
import com.example.dome.engine.OrderBook;
import com.example.dome.model.Order;
import com.example.dome.model.OrderSide;
import com.example.dome.model.OrderType;
import com.example.dome.persistence.TradeRepository;
import com.example.dome.security.ApiKeyFilter;
import com.example.dome.security.RateLimitFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class, 
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {ApiKeyFilter.class, RateLimitFilter.class}))
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.example.dome.engine.EngineRegistry engineRegistry;
    
    // We still mock MatchingEngine to return from Registry
    private MatchingEngine matchingEngine;

    @MockBean
    private TradeRepository tradeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private com.lmax.disruptor.dsl.Disruptor<com.example.dome.engine.disruptor.OrderCommand> disruptor;

    @Test
    public void testPlaceOrder() throws Exception {
        OrderRequest request = new OrderRequest();
        request.setSymbol("AAPL");
        request.setSide(OrderSide.BUY);
        request.setType(OrderType.LIMIT);
        request.setPrice(new BigDecimal("150.00"));
        request.setQuantity(100);

        matchingEngine = org.mockito.Mockito.mock(MatchingEngine.class);
        when(engineRegistry.getEngine("AAPL")).thenReturn(matchingEngine);
        
        // Mock Disruptor behavior
        org.mockito.Mockito.doAnswer(invocation -> {
            com.lmax.disruptor.EventTranslator<com.example.dome.engine.disruptor.OrderCommand> translator = 
                invocation.getArgument(0);
            
            // Create dummy event and translate
            com.example.dome.engine.disruptor.OrderCommand command = new com.example.dome.engine.disruptor.OrderCommand();
            translator.translateTo(command, 0);
            
            // Simulate processing and complete future
            if (command.getResultFuture() != null) {
                command.getResultFuture().complete(new ArrayList<>());
            }
            return null;
        }).when(disruptor).publishEvent(any(com.lmax.disruptor.EventTranslator.class));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
    
    @Test
    public void testGetOrderBook() throws Exception {
        OrderBook mockBook = new OrderBook("AAPL");
        matchingEngine = org.mockito.Mockito.mock(MatchingEngine.class);
        when(engineRegistry.getEngine("AAPL")).thenReturn(matchingEngine);
        when(matchingEngine.getOrderBook()).thenReturn(mockBook);
        
        mockMvc.perform(get("/api/orderbook/AAPL"))
                .andExpect(status().isOk());
    }
}
