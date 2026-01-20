# Distributed Order Matching Engine (D.O.M.E.)

A high-performance, low-latency Limit Order Book (LOB) matching engine built with Java Spring Boot.

## Features

- **Core Matching Engine**:
  - Price-Time Priority (FIFO) matching algorithm.
  - Supports LIMIT and MARKET orders.
  - Supports Partial Fills.
- **High Performance**:
  - **LMAX Disruptor**: Ring-buffer based event sourcing for lock-free order processing.
  - **JCTools**: Optimized non-blocking queues for internal data structures.
  - **In-Memory**: `TreeMap` based order book for O(log n) operations.
- **Persistence & Recovery**:
  - **RocksDB**: Local persistent key-value store for order state recovery (WAL).
  - **H2/Postgres**: Relational DB for trade history queries.
- **API & Streaming**:
  - **REST API**: Order management (Place, Cancel, View Book).
  - **WebSocket (STOMP)**: Real-time public feed of trade execution and book updates.
  - **Analytics**: CSV export of trade history for analysis.

## Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.2
- **Concurrency**: LMAX Disruptor, JCTools
- **Storage**: RocksDB (Log), H2 (Trades), Redis (Cache)
- **Testing**: JUnit 5, Mockito, Gatling (Load Testing)

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker (optional)

### Running Locally

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/your-username/distributed-order-matching-engine.git
    cd distributed-order-matching-engine
    ```

2.  **Build the project**:
    ```bash
    mvn clean install
    ```

3.  **Run the application**:
    ```bash
    mvn spring-boot:run
    ```

4.  **Access the API**:
    - Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
    - WebSocket: `ws://localhost:8080/ws`

### Running with Docker

1.  **Build and Start**:
    ```bash
    docker-compose up --build
    ```

2.  The application will be available at `http://localhost:8080`.

## API Usage

### Place Order
`POST /api/orders`
```json
{
  "type": "LIMIT",
  "side": "BUY",
  "symbol": "AAPL",
  "price": 150.50,
  "quantity": 100
}
```

### Get Order Book
`GET /api/orderbook/AAPL`

### Analytics Export
`GET /api/analytics/trades/csv`

## Performance Testing

Run the **Gatling** load test simulation:
```bash
mvn gatling:test
```
