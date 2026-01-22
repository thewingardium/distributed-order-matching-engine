# Low-Level Design (LLD) Interview Guide
## Distributed Order Matching Engine

This guide breaks down the application architecture, design choices, and flow to help you present it confidently in a System Design or LLD interview.

---

## 1. Requirements

### Functional Requirements (What it does)
1.  **Order Placement**: Users can place `LIMIT` and `MARKET` orders.
2.  **Order Matching**: The system matches Buy and Sell orders based on **Price-Time Priority** (FIFO).
3.  **Order Book Management**: Maintains a real-time book of open orders (Bids vs Asks).
4.  **Trade Execution**: Generates trade records when a match occurs.
5.  **Persistence**: Saves orders and trades to disk to survive restarts.
6.  **Querying**: Users can fetch the current Order Book and Trade History.

### Non-Functional Requirements (System Qualities)
1.  **Low Latency**: Matching must remain fast (microseconds/milliseconds) under load.
    *   *Solution*: In-memory matching, LMAX Disruptor for lock-free concurrency.
2.  **Throughput**: Must handle high concurrent request volume.
    *   *Solution*: Async processing, event-driven architecture.
3.  **Thread Safety**: `OrderBook` must handle concurrent reads/writes without corruption.
    *   *Solution*: `ReentrantReadWriteLock` and Single-Threaded Writer (via Disruptor).
4.  **Durability**: No data loss on crash.
    *   *Solution*: Hybrid Persistence (RocksDB for active state, PostgreSQL for history).

---

## 2. Core Entities (The "Nouns")

### A. Order
The fundamental unit of the system.
*   **Attributes**: `ID`, `Symbol` (e.g., AAPL), `Side` (Buy/Sell), `Type` (Limit/Market), `Price`, `Quantity`, `Status`.
*   **Key Design Choice**: It is mutable (status/quantity changes) but carefully controlled within the core engine.

### B. OrderBook
The "Container" for all active orders for a single symbol.
*   **Structure**:
    *   **Bids (Buys)**: `TreeMap<Price, PriceLevel>` (Descending Order - Highest Bid needed first).
    *   **Asks (Sells)**: `TreeMap<Price, PriceLevel>` (Ascending Order - Lowest Ask needed first).
    *   **Index**: `HashMap<UUID, Order>` for O(1) lookup during cancellation.
*   **Why TreeMap?**: Provides O(log n) insertion/deletion naturally ordered by price, which is essential for matching logic.

### C. PriceLevel
Represents all orders at a specific price point (e.g., all Buys at $150.00).
*   **Structure**: A FIFO Queue (List/Deque) of Orders.
*   **Logic**: Enforces "Time Priority". The order that arrived first at this price is executed first.

### D. Trade
Immutable record of a successful match.
*   **Attributes**: `MakerOrderId`, `TakerOrderId`, `Price`, `Quantity`, `Timestamp`.

---

## 3. Component Architecture (The "Verbs")

### A. EngineRegistry
*   **Role**: The Router.
*   **Behavior**: Manages a Map of `Symbol -> MatchingEngine`. Looks up the correct engine for an incoming order.

### B. MatchingEngine
*   **Role**: The Core Logic Processor.
*   **Behavior**:
    1.  Receives an Order.
    2.  traverse the opposite side of the book (e.g., if Buy, look at Asks).
    3.  Matches until the order is filled or the book runs out.
    4.  Adds remaining quantity to the book (if Limit order).

### C. LMAX Disruptor (Crucial Pattern)
*   **Role**: The Traffic Cop.
*   **Behavior**: A high-performance Inter-Thread Messaging Library.
*   **Why?**: Standard Blocking Queues lock. Disruptor uses a "Ring Buffer" and `CompareAndSwap` (CAS) operations for zero-lock latency.
*   **Pipeline**: `Input` -> `RingBuffer` -> `MatchingHandler` (In-Memory) -> `PersistenceHandler` (Disk) -> `Output`.

---

## 4. The "Life of a Request" (Request Flow)

This is the most important part of the interview. Walk them through `POST /orders`.

### Step 1: Request Origination (API Layer)
*   **User**: Sends `POST /api/orders` JSON.
*   **Component**: `OrderController`.
*   **Action**:
    1.  Validates input (Positive quantity? Valid Symbol?).
    2.  Creates an `Order` object with status `NEW`.
    3.  **Publish**: Pushes the order into the **Disruptor RingBuffer**.
    *   *Note*: The HTTP thread returns "Processing" immediately or waits on a Future (Async vs Sync).

### Step 2: The Ring Buffer (Sequencing)
*   **Component**: `Disruptor`.
*   **Action**: Assigns a sequence number to the event. Ensures global ordering of all incoming requests.

### Step 3: Core Processing (Single Threaded Consumer)
*   **Component**: `MatchingEventHandler`.
*   **Action**:
    1.  Picks up the Limit Order event.
    2.  Calls `MatchingEngine.match(order)`.
    3.  **Matching Logic**:
        *   Checks Best Ask > Limit Price? No match -> Add to Book.
        *   Checks Best Ask <= Limit Price? Match! -> Create `Trade` object, reduce Ask quantity.
    4.  Updates `Order` status to `FILLED` or `PARTIALLY_FILLED`.

### Step 4: Persistence & Notification
*   **Component**: `PersistenceEventHandler`.
*   **Action**:
    1.  **RocksDB**: Updates the "Active Order" state (fast KV store).
    2.  **SQL**: Inserts the `Trade` record for history.
    3.  **EventProcessor**: Publishes updates to WebSocket topics (e.g., `/topic/orderbook/AAPL`).

### Step 5: The Response
*   The `OrderController` (waiting on the Future) receives the result and sends HTTP 200 Back to user.

---

## 5. Key Design Patterns to Mention

1.  **Producer-Consumer Pattern**: Decouples the API (Thread per request) from the Engine (Single Thread processing) via the Disruptor.
2.  **Strategy Pattern**: Used in `MatchingAlgorithm` (allows swapping FIFO for Pro-Rata later).
3.  **Repository Pattern**: Hides storage complexity (`OrderDao`, `TradeRepository`) from the domain logic.
4.  **DTO (Data Transfer Object)**: `OrderRequest` is separate from the `Order` entity to decouple API contract from internal state.
5.  **Singleton**: `EngineRegistry` is a Singleton managing the engines.

---

## 6. Common Interview Questions (Design Defenses)

### Q: Why `TreeMap` instead of `PriorityQueue` (Heap) for the OrderBook?
**A:** This is a classic question.
1.  **Random Access (Cancellation)**:
    *   **Scenario**: A user cancels an order that is *not* at the best price (e.g., deep in the book).
    *   **PriorityQueue**: Finding an arbitrary element to remove is **O(N)**.
    *   **TreeMap**: Deleting a price level is **O(log N)**.
2.  **Price Grouping**:
    *   We match by **Price Level** (all orders at $150.00).
    *   When a new order comes in at $150.00, we need to find the existent bucket for $150.00.
    *   **PriorityQueue**: Searching for an existing node is **O(N)**.
    *   **TreeMap**: `get(price)` is **O(log N)**.
3.  **Market Data (Snapshots)**:
    *   We need to stream the "Top 10 Levels" to the frontend.
    *   **PriorityQueue**: Getting the top K elements requires dequeuing (destructive) or copying, which is expensive.
    *   **TreeMap**: Iterating `entrySequence()` is efficient and ordered.

### Q: Why H2 for development but RocksDB for production state?
**A:**
*   **H2 (In-Memory)**: Great for testing relational queries during dev.
*   **RocksDB**: A high-performance Key-Value store. It's much faster for writing simple Order blobs (byte arrays) than a full SQL INSERT, which is critical for the "Persistence Handler" in a low-latency pipeline.

