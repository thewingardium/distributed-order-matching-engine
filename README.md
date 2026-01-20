# Distributed Order Matching Engine

A high-performance, low-latency Distributed Order Matching Engine built with Spring Boot, optimized for concurrency using LMAX Disruptor and RocksDB for persistence.

## Architecture

![Architecture](https://via.placeholder.com/800x400?text=System+Architecture+Placeholder)

The system consists of the following core components:
- **Order Gateway (REST/WebSocket)**: Handles incoming order requests and streams market data.
- **Matching Engine**: Core logic for order matching (Price-Time Priority) using **LMAX Disruptor** for lock-free processing.
- **Persistence Layer**: 
    - **RocksDB**: Low-latency key-value storage for active order state.
    - **PostgreSQL/TimescaleDB**: Relational storage for Trade history and analytics.
- **Caching**: **Redis** is used to cache the Order Book (Best Bid/Ask) for fast read access.
- **Analytics**: Python-based pipeline (`scripts/analyze_market.py`) for visualizing trade data.

## Features

- **High Performance**: Lock-free order matching with LMAX Disruptor.
- **Persistence & Recovery**: Fast recovery from disk (RocksDB) on startup.
- **Real-time Streaming**: WebSocket (STOMP) support for live order book updates.
- **Multi-Symbol Support**: Separate matching engines for different symbols (AAPL, GOOGL, etc.).
- **Security**: API Key authentication and Rate Limiting (Bucket4j).
- **Analytics**: Automated trade history export and visualization.

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose (optional, for containerized run)

## How to Run

### 1. Local Development

Prerequisites: Ensure Redis is running (default localhost:6379) or use the embedded H2 database config.

```bash
# Build the application
mvn clean install

# Run the application
java -jar target/distributed-order-matching-engine-0.0.1-SNAPSHOT.jar
```

### 2. Docker (Recommended)

This spins up the Application and Redis automatically.

```bash
docker-compose up --build -d
```

Check status:
```bash
docker-compose ps
```

Stop:
```bash
docker-compose down
```

## API Documentation

Once running, access the Swagger UI documentation:
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Key Endpoints

- **POST /api/orders**: Place a new order.
- **GET /api/orderbook/{symbol}**: Get current order book.
- **GET /api/trades**: Query trade history.
- **GET /api/analytics/export/csv**: Export trades to CSV.

### WebSocket

- **Endpoint**: `/ws`
- **Topic**: `/topic/orderbook/{symbol}`

## Analytics

To run the Python analytics script:

```bash
cd scripts
pip install -r requirements.txt
python analyze_market.py
```

## Configuration

- `src/main/resources/application.properties`: Main configuration (H2, Logging, Rate Limits).
- `Dockerfile`: Multi-stage Docker build.

