# User Task List: Manual Verification & Operation

Use this checklist to verify your environment and run the matching engine.

## 1. Environment Setup
- [ ] **Verify Java 17+**: Run `java -version` in your terminal.
- [ ] **Verify Maven**: Run `mvn -version`.
- [ ] **Start Redis**:
    - Ensure Redis is running on port 6379.
    - If using Docker: `docker-compose up -d redis`
- [ ] **Install Python Dependencies** (for scripts):
    - Run: `pip install requests pandas matplotlib`

## 2. Start the Engine
- [ ] **Run Application**:
    - Build: `mvn clean install -DskipTests`
    - Run: `java -jar target/distributed-order-matching-engine-0.0.1-SNAPSHOT.jar`
    - *Success Indicator*: Look for "Started DistributedOrderMatchingEngineApplication" in logs.

## 3. Run Custom Data Test
- [ ] **Locate Test Data**: Check `data/test_orders.csv`.
- [ ] **Run Replay Script**:
    - Open terminal in project root.
    - Run: `python scripts/replay_orders.py`
    - *Success Indicator*: You should see a stream of "SUCCESS" messages for order placement.

## 4. Verify Results
- [ ] **Check Order Book**:
    - Open Browser: `http://localhost:8080/api/orderbook/AAPL`
    - Verify you see Bids and Asks populated.
- [ ] **Check Trades**:
    - Open Browser: `http://localhost:8080/api/trades`
    - Verify executed trades appear.

## 5. High-Speed Load Test (Optional)
- [ ] **Run Gatling**:
    - Run: `mvn gatling:test`
    - This will stress test the engine with random data.
