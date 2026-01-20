package com.example.dome.analytics;

import com.example.dome.model.Trade;
import com.example.dome.persistence.TradeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        com.example.dome.security.ApiKeyFilter.class, 
        com.example.dome.security.RateLimitFilter.class
    }))
public class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradeRepository tradeRepository;

    @Test
    public void testExportCsv() throws Exception {
        Trade t1 = Trade.builder()
            .tradeId(UUID.randomUUID())
            .symbol("AAPL")
            .price(new BigDecimal("150.00"))
            .quantity(10)
            .buyOrderId(UUID.randomUUID())
            .sellOrderId(UUID.randomUUID())
            .timestamp(Instant.now())
            .build();

        when(tradeRepository.findAll()).thenReturn(Arrays.asList(t1));

        mockMvc.perform(get("/api/analytics/trades/csv"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/csv"));
    }
}
