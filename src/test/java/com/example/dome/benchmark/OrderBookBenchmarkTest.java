package com.example.dome.benchmark;

import org.junit.jupiter.api.Test;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class OrderBookBenchmarkTest {

    @Test
    public void runJmhBenchmark() throws Exception {
        Options opt = new OptionsBuilder()
                .include(OrderBookBenchmark.class.getSimpleName())
                // Use 0 forks for faster execution in test environment (but less accurate)
                // Or 1 fork if possible.
                .forks(1) 
                .warmupIterations(1)
                .measurementIterations(1)
                .build();

        new Runner(opt).run();
    }
}
