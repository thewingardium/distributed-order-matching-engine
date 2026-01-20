package com.example.dome.performance;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class LoadTestSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("application/json")
        .contentTypeHeader("application/json");

    // Random Order Generator
    Iterator<Map<String, Object>> feeder = Stream.generate((Supplier<Map<String, Object>>) () -> {
        Random rand = new Random();
        String side = rand.nextBoolean() ? "BUY" : "SELL";
        // Price around 100-200
        double price = 100 + rand.nextDouble() * 100; 
        int quantity = 1 + rand.nextInt(100);
        
        return Map.of(
            "symbol", "AAPL",
            "side", side,
            "type", "LIMIT",
            "price", String.format("%.2f", price),
            "quantity", quantity
        );
    }).iterator();

    ScenarioBuilder scn = scenario("Trading Scenario")
        .feed(feeder)
        .exec(
            http("Place Limit Order")
            .post("/api/orders")
            .body(StringBody("{\"symbol\": \"#{symbol}\", \"side\": \"#{side}\", \"type\": \"#{type}\", \"price\": \"#{price}\", \"quantity\": #{quantity}}"))
            .check(status().is(200))
        );

    {
        setUp(
            scn.injectOpen(
                // Warm up
                rampUsers(10).during(Duration.ofSeconds(10)),
                // Stress test: 1000 users over 60 seconds? No, that's low.
                // We want Throughput. 
                // constantUsersPerSec(20).during(Duration.ofSeconds(20))
                
                // Ramp up to high load
                rampUsersPerSec(10).to(100).during(Duration.ofSeconds(30)),
                constantUsersPerSec(100).during(Duration.ofSeconds(60)) 
            )
        ).protocols(httpProtocol);
    }
}
