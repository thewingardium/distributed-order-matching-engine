package com.example.dome.analytics;

import com.example.dome.model.Trade;
import com.example.dome.persistence.TradeRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final TradeRepository tradeRepository;

    @GetMapping("/trades/csv")
    public void exportTradesCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"trades.csv\"");
        
        List<Trade> trades = tradeRepository.findAll();
        
        try (PrintWriter writer = response.getWriter()) {
            // Header
            writer.println("trade_id,symbol,price,quantity,buyer_order_id,seller_order_id,timestamp");
            
            // Data
            for (Trade trade : trades) {
                writer.printf("%s,%s,%s,%d,%s,%s,%s%n",
                    trade.getTradeId(),
                    trade.getSymbol(),
                    trade.getPrice(),
                    trade.getQuantity(),
                    trade.getBuyOrderId(),
                    trade.getSellOrderId(),
                    trade.getTimestamp().toString()
                );
            }
        }
    }
}
