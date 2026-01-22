# Deployment & Configuration Guide

This guide explains how to configure the **Distributed Order Matching Engine** for different environments, focusing on connecting to external services like **TimescaleDB** (PostgreSQL) and **Redis**.

## 1. Local Development (Docker)

For local development, we recommend using Docker Compose to spin up the dependencies automatically.

### Prerequisites
- Docker & Docker Compose installed.

### Configuration
The `docker-compose.yml` file sets up:
- **Redis** on port `6379`.
- **H2 Database** (In-Memory) for the application logic by default.
- *Optional*: You can add a PostgreSQL service to `docker-compose.yml` if you want to test TimescaleDB locally.

**Running:**
```bash
docker-compose up -d
```

**Connection String (Internal Docker Network):**
- Redis Host: `redis` (Service name in docker-compose)
- Port: `6379`

## 2. AWS Cloud Deployment

To deploy on AWS, you will typically use **Amazon ElastiCache for Redis** and **Amazon RDS for PostgreSQL/TimescaleDB**.

### A. Redis (ElastiCache) Configuration
1. **Create ElastiCache Cluster**:
   - Go to AWS Console -> ElastiCache -> Redis.
   - Create a cluster (ensure it is in the same VPC/Subnet as your application).
   - Copy the **Primary Endpoint** (e.g., `my-cluster.xxxxxx.use1.cache.amazonaws.com`).

2. **Update `application.properties`**:
You can pass these as Environment Variables to your container or EC2 instance.

```properties
# Spring Data Redis
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
# If using password/auth
# spring.data.redis.password=${REDIS_PASSWORD}
# spring.data.redis.ssl=true  # Enable if ElastiCache has Encryption in Transit enabled
```

### B. TimescaleDB (RDS PostgreSQL) Configuration
1. **Create RDS Instance**:
   - Go to AWS Console -> RDS -> Create Database (PostgreSQL).
   - Ensure you enable the **TimescaleDB** extension if allowed, or use a standard Postgres instance for trade history.
   - Make sure Security Groups allow traffic on port `5432` from your application.

2. **Update `application.properties`**:
Disable H2 and enable PostgreSQL config.

```properties
# Disable H2
spring.h2.console.enabled=false

# PostgreSQL / TimescaleDB
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:5432/${DB_NAME:dome_db}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD:password}
spring.datasource.driver-class-name=org.postgresql.Driver

# Hibernate/JPA
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
```

### 3. Usage via Environment Variables (Recommended)
Do not hardcode production credentials in `application.properties`. Use the placeholders above (`${VAR_NAME:default}`) and set the environment variables when running the app.

**Example Command (EC2 / ECS):**
```bash
export REDIS_HOST=my-cluster.axxx.use1.cache.amazonaws.com
export DB_HOST=my-db-instance.rds.amazonaws.com
export DB_USER=admin
export DB_PASSWORD=securepass

java -jar app.jar
```

## 4. Connectivity Troubleshooting
- **Security Groups (AWS)**: Ensure your EC2/ECS Security Group allows Outbound on 6379 & 5432. Ensure RDS/ElastiCache Security Groups allow Inbound from your App's Security Group.
- **VPC**: Ensure both App and Data layers are in the same VPC or have Peering established.

## 5. Hybrid Setup: Local Java + AWS Data Services
**Scenario**: You run the Java JAR on your laptop, but connect to AWS RDS and AWS ElastiCache.

### Challenge
AWS RDS and ElastiCache are typically deployed in **Private Subnets** for security, meaning they have no public IP address and cannot be reached directly from your laptop.

### Solution A: SSH Tunneling (Step-by-Step Guide)

This is the standard, secure way to connect your local machine to private AWS resources using a "Jump box" or "Bastion".

#### Phase 1: AWS Setup (One Time)
1.  **Launch EC2 Instance (The Bastion)**:
    *   **AMI**: Amazon Linux 2023 (Free Tier eligible).
    *   **Type**: `t3.nano` or `t2.micro`.
    *   **Network**: Must be in the **Public Subnet** of your VPC.
    *   **Auto-assign Public IP**: Enable.
    *   **Key Pair**: Create a new one (e.g., `dome-key.pem`) and download it.

2.  **Configure Security Groups**:
    *   **Bastion SG**: Allow **Inbound SSH (Port 22)** -> Source: `My IP` (Your laptop).
    *   **RDS & ElastiCache SGs**: Allow **Inbound (Ports 5432 & 6379)** -> Source: **Custom** -> Select the **Bastion SG** ID (e.g., `sg-01234...`).
    *   *Why?* This allows the Bastion to talk to the DB, and You to talk to the Bastion.

#### Phase 2: Connect (Every Time)
1.  **Prepare Key**:
    *   Place `dome-key.pem` in your project folder.
    *   (Linux/Mac) Run: `chmod 400 dome-key.pem` to secure it.

2.  **Gather Info**:
    *   **Bastion IP**: Go to EC2 Console -> Copy `Public IPv4 address`.
    *   **Redis Endpoint**: ElastiCache -> Redis -> Cluster -> Copy `Primary Endpoint` (remove `:6379` port if included).
    *   **DB Endpoint**: RDS -> Databases -> Instance -> Connectivity -> Copy `Endpoint`.

3.  **Run Tunnel Command**:
    Open a dedicated terminal and run:

    ```bash
    # Structure: ssh -i key.pem -L localPort:remoteHost:remotePort ec2-user@bastion-ip
    
    ssh -i dome-key.pem -N \
        -L 6379:my-redis-cluster.ab123.use1.cache.amazonaws.com:6379 \
        -L 5432:my-db-instance.ab123.use1.rds.amazonaws.com:5432 \
        ec2-user@54.123.45.67
    ```

    *   `-i`: Identity file (your key).
    *   `-N`: "Do not execute a remote command" (just forward ports).
    *   `-L`: Local forwarding. `6379:target:6379` means "Forward my localhost:6379 to target:6379".

    **Success**: The command will hang and show no output. This is normal! It means the tunnel is open.

#### Phase 3: Run Application
Now, your local machine "thinks" Redis and Postgres are running locally.

1.  **Test Connection** (Optional):
    *   Redis: `telnet localhost 6379` (Should connect).
2.  **Start App**:
    ```bash
    java -jar target/distributed-order-matching-engine-0.0.1-SNAPSHOT.jar
    ```
    (No need to change config, as `localhost` is the default).

### Solution B: Public Access (Less Secure)
1.  **Modify AWS Services**:
    *   **RDS**: Modify the instance -> Connectivity -> **Publicly Accessible: Yes**.
    *   **ElastiCache**: *Note: ElastiCache does not support Public IPs directly easily. You usually MUST use Tunneling/VPN for Redis.*
2.  **Allow Your IP**:
    *   Add your laptop's Public IP to the RDS Security Group for port 5432.
3.  **Run Application**:
    ```bash
    export DB_URL=jdbc:postgresql://my-rds-db.xxxx.amazonaws.com:5432/dome_db
    export DB_USER=postgres
    export DB_PASSWORD=mypassword
    
    # Redis will likely fail unless you use Solution A
    java -jar target/distributed-order-matching-engine-0.0.1-SNAPSHOT.jar
    ```
