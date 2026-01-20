package com.example.dome.config;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

@Configuration
public class RocksDBConfig {

    @org.springframework.beans.factory.annotation.Value("${rocksdb.path:data/rocksdb/orders}")
    private String dbPath;

    static {
        RocksDB.loadLibrary();
    }

    @Bean
    public RocksDB rocksDB() throws IOException, RocksDBException {
        // Ensure directory exists
        File dbDir = new File(dbPath);
        if (!dbDir.exists()) {
             if (!dbDir.mkdirs()) {
                 throw new IOException("Could not create RocksDB directory: " + dbPath);
             }
        }

        Options options = new Options().setCreateIfMissing(true);
        return RocksDB.open(options, dbPath);
    }
}
