# Distributed Order Matching Engine - UML Class Diagram

```mermaid
classDiagram
    %% Core Controller & API
    class OrderController {
        -EngineRegistry engineRegistry
        -TradeRepository tradeRepository
        -Disruptor~OrderCommand~ disruptor
        +placeOrder(OrderRequest)
        +cancelOrder(UUID)
        +getOrderBook(String)
    }

    class EngineRegistry {
        -Map~String, MatchingEngine~ engines
        +getEngine(String)
        +cancelOrder(UUID)
        +recoverState()
    }

    %% Engine Core
    class MatchingEngine {
        -String symbol
        -OrderBook orderBook
        -MatchingAlgorithm matchingAlgorithm
        -EventProcessor eventProcessor
        -OrderDao orderDao
        +match(Order)
        +processOrder(Order)
        +cancelOrder(UUID)
    }

    class OrderBook {
        -TreeMap~BigDecimal, PriceLevel~ bids
        -TreeMap~BigDecimal, PriceLevel~ asks
        -Map~UUID, Order~ orderIndex
        +addOrder(Order)
        +cancelOrder(Order)
        +getSnapshot()
    }

    class PriceLevel {
        -BigDecimal price
        -List~Order~ orders
        +addOrder(Order)
        +match(Order)
    }

    class MatchingAlgorithm {
        <<interface>>
        +match(Order, OrderBook)
    }
    class FifoMatchingAlgorithm {
        +match(Order, OrderBook)
    }

    %% LMAX Disruptor Pattern
    class DisruptorConfig {
        +disruptor()
    }
    class OrderCommand {
        -Order order
        -MatchResult result
    }
    class MatchingEventHandler {
        -EngineRegistry registry
        +onEvent(OrderCommand)
    }
    class PersistenceEventHandler {
        -OrderDao orderDao
        +onEvent(OrderCommand)
    }

    %% Models
    class Order {
        -UUID orderId
        -String symbol
        -OrderSide side
        -BigDecimal price
        -long quantity
    }
    class Trade {
        -UUID tradeId
        -String symbol
        -BigDecimal price
        -long quantity
    }

    %% Persistence & Event
    class OrderDao {
        <<interface>>
        +save(Order)
        +findById(String)
    }
    class RocksDBOrderDao {
        -RocksDB db
        +save(Order)
    }
    class EventProcessor {
        -SimpMessagingTemplate websocket
        +onTrade(TradeEvent)
        +onBookUpdate(String, BigDecimal, BigDecimal)
    }

    %% Relationships
    OrderController --> EngineRegistry : uses
    OrderController --> DisruptorConfig : publishes to
    
    EngineRegistry *-- MatchingEngine : manages
    
    MatchingEngine *-- OrderBook : owns
    MatchingEngine --> MatchingAlgorithm : uses
    MatchingEngine --> EventProcessor : notifies
    MatchingEngine --> OrderDao : uses

    OrderBook *-- PriceLevel : contains
    
    FifoMatchingAlgorithm ..|> MatchingAlgorithm : implements

    %% Disruptor Flow
    DisruptorConfig --> MatchingEventHandler : triggers
    MatchingEventHandler --> PersistenceEventHandler : then
    MatchingEventHandler --> EngineRegistry : delegates matching
    PersistenceEventHandler --> OrderDao : persists

    RocksDBOrderDao ..|> OrderDao : implements

```

## Diagram Overview
- **Controller Layer**: Handles HTTP requests and dispatches orders to the LMAX Disruptor ring buffer.
- **Engine Layer**: `MatchingEngine` is the core, managing the `OrderBook`. It supports multiple symbols via `EngineRegistry`.
- **Infrastructure**:
    - **Disruptor**: Asynchronous event processing pipeline (`MatchingEventHandler` -> `PersistenceEventHandler`).
    - **Persistence**: Hybrid approach using **RocksDB** (Fast Key-Value for active orders) and **PostgreSQL** (Trades).
