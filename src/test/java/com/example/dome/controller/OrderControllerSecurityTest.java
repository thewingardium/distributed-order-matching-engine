package com.example.dome.controller;

import com.example.dome.engine.MatchingEngine;
import com.example.dome.persistence.TradeRepository;
import com.example.dome.security.ApiKeyFilter;
import com.example.dome.security.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
@Import({ApiKeyFilter.class, RateLimitFilter.class})
public class OrderControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.example.dome.engine.EngineRegistry engineRegistry;

    @MockBean
    private TradeRepository tradeRepository;

    @MockBean
    private com.lmax.disruptor.dsl.Disruptor<com.example.dome.engine.disruptor.OrderCommand> disruptor;

    @Test
    public void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/orderbook/AAPL"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testAuthorizedAccess() throws Exception {
        // Mock Registry to return an engine
        MatchingEngine mockEngine = org.mockito.Mockito.mock(MatchingEngine.class);
        org.mockito.Mockito.when(engineRegistry.getEngine("AAPL")).thenReturn(mockEngine);
        // Mock getOrderBook to return null so Controller returns 404
        org.mockito.Mockito.when(mockEngine.getOrderBook()).thenReturn(null);
        
        mockMvc.perform(get("/api/orderbook/AAPL")
                .header("X-API-KEY", "test-api-key"))
                .andExpect(status().isNotFound());
    }
}
