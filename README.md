# Smart Order System
## Distributed Querying in SOA - Problems & CQRS-based Solutions

---

## 📐 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         API Gateway :8080                           │
│  (API Composition: aggregates data from multiple services)          │
└─────────────┬────────────────────────┬──────────────────────────────┘
              │ Feign                  │ Feign
              ▼                        ▼
┌─────────────────────┐    ┌──────────────────────────┐
│  user-service :8081 │    │ order-query-service :8084 │
│  DB: user_db        │    │ DB: order_read_db          │
│  (mysql-main:3306)  │    │ (mysql-read:3308)          │
└─────────────────────┘    └──────────┬───────────────┘
                                      │ consume events
                        RabbitMQ      │
                    ┌─────────────────┘
                    │
        ┌───────────▼──────────────────────┐
        │    order-command-service :8083    │
        │    DB: order_write_db             │
        │    (mysql-write:3307)             │
        └──────────────────────────────────┘
              │ publish OrderCreatedEvent
              ▼
         [RabbitMQ :5672]
```

---

## 🚀 Quick Start

### 1. Start Infrastructure

```bash
docker-compose up -d
```

Wait for all containers to be healthy (~30s).

### 2. Build & Run Services

```bash
# Build all
mvn clean package -DskipTests

# Run each service (in separate terminals)
java -jar user-service/target/user-service-1.0.0.jar
java -jar product-service/target/product-service-1.0.0.jar
java -jar order-command-service/target/order-command-service-1.0.0.jar
java -jar order-query-service/target/order-query-service-1.0.0.jar
java -jar api-gateway/target/api-gateway-1.0.0.jar
```

### 3. Seed Demo Data

```bash
# Seed users
curl -X POST http://localhost:8081/users/seed

# Seed products
curl -X POST http://localhost:8082/products/seed

# Seed 100 orders for user 1 (to make benchmark visible)
curl -X POST http://localhost:8083/orders/seed/1/100
```

---

## 🧪 Demo Scenarios

### A. Distributed Query + API Composition

```bash
# Get user + order summary - triggers 2 Feign calls internally
curl http://localhost:8080/users/1/order-summary
```

**What happens internally:**
1. Gateway → `GET http://user-service:8081/users/1`
2. Gateway → `GET http://order-query-service:8084/order-summary/1`
3. Gateway merges both responses
4. Console shows individual + total latency

### B. Create an Order (CQRS Command + Event Flow)

```bash
curl -X POST http://localhost:8083/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 1,
    "quantity": 2,
    "totalAmount": 50000000
  }'
```

**Watch the logs:**
- `order-command-service`: `[COMMAND] Order saved to write DB`
- `order-command-service`: `[EVENT] OrderCreatedEvent published to RabbitMQ`
- `order-query-service`: `[EVENT RECEIVED] OrderCreatedEvent`
- `order-query-service`: `[READ MODEL UPDATED] userId=1 | totalOrders=...`

### C. Benchmark: Write DB Count vs Read Model

```bash
# Compare both approaches side by side
curl http://localhost:8080/benchmark/compare/1
```

Individual benchmarks:
```bash
# Write DB: COUNT(*) on orders table
curl http://localhost:8083/benchmark/write-count/1

# Read Model: single row lookup
curl http://localhost:8084/benchmark/read-model/1
```

---

## 🧠 Key Concepts Explained

### 1. Distributed Query Problem

**In a Monolith:**
```sql
-- Single query, single transaction ✅
SELECT u.id, u.name, COUNT(o.id) as total_orders, SUM(o.amount) as total_amount
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE u.id = 1
GROUP BY u.id, u.name;
```

**In Microservices (each service has its own DB):**
```
user_db    → owned by user-service
order_db   → owned by order-service

❌ CANNOT do: SELECT * FROM user_db.users JOIN order_db.orders
   (different databases, possibly different servers/vendors)
```

### 2. API Composition (Solution & Trade-offs)

**Solution:** API Gateway calls each service separately and merges results.

```java
// GatewayController.java
UserDto user = userServiceClient.getUserById(id);         // HTTP call 1
OrderSummaryDto orders = orderQueryServiceClient.getOrderSummary(id); // HTTP call 2
// Merge and return
```

| Aspect         | Monolith DB JOIN     | API Composition          |
|----------------|----------------------|--------------------------|
| Latency        | ~2-5ms              | ~20-200ms (network hops) |
| Coupling       | Tight (shared DB)    | Loose (HTTP contracts)   |
| Scalability    | Limited (shared DB)  | Independent scaling      |
| Availability   | Single point         | Compounded (A₁ × A₂)    |
| Consistency    | Strong (ACID)        | Eventually consistent    |
| Complexity     | Simple               | Complex (fault tolerance)|

### 3. CQRS (Command Query Responsibility Segregation)

```
Traditional CRUD:
Service → single database ← reads and writes

CQRS:
Commands → order-command-service → order_write_db (normalized)
                    ↓
               [RabbitMQ Event]
                    ↓
Queries  ← order-query-service ← order_read_db (denormalized, pre-aggregated)
```

**Benefits:**
- Write model can use different schema optimized for consistency
- Read model can use denormalized schema optimized for queries
- Each side scales independently
- Read replicas can be added without affecting write performance

### 4. Eventual Consistency

```
Timeline:
T0: User creates order
T1: order-command-service saves to order_write_db ✅
T2: OrderCreatedEvent published to RabbitMQ
T3: [network delay ~1-100ms]
T4: order-query-service receives event
T5: order_read_db updated ✅

During T1→T5: Read model is STALE
After T5: Read model is CONSISTENT
```

**Is this acceptable?**
- Dashboard showing total orders: YES (1-2 second lag is fine)
- Inventory check before purchase: NO (need strong consistency)
- Bank transfer confirmation: NO (need ACID)

### 5. Read Model Optimization

**Without CQRS (direct write DB query):**
```sql
-- Must scan entire orders table every time
SELECT COUNT(*), SUM(total_amount)
FROM orders
WHERE user_id = 1;
-- O(n) complexity where n = total orders for user
-- With 1M orders: full table scan, ~500ms
```

**With CQRS Read Model:**
```sql
-- Pre-computed, single row lookup
SELECT total_orders, total_amount
FROM order_summary_view
WHERE user_id = 1;
-- O(1) with index, always ~1-2ms
-- Doesn't matter if user has 1 or 1,000,000 orders
```

**How read model stays updated:**
```java
// OrderEventListener.java - runs on every OrderCreatedEvent
summary.setTotalOrders(summary.getTotalOrders() + 1);      // increment
summary.setTotalAmount(summary.getTotalAmount().add(event.getTotalAmount())); // add
orderSummaryViewRepository.save(summary);
// Much faster than recalculating from scratch!
```

---

## 📊 Performance Analysis

### Latency

| Operation                  | Approach              | Expected Latency     |
|----------------------------|-----------------------|----------------------|
| Get user + order summary   | API Composition       | 20-200ms             |
| Get user + order summary   | Monolith DB JOIN      | 2-10ms               |
| Order count for user       | Write DB COUNT(*)     | Grows with data size |
| Order count for user       | Read Model lookup     | ~1-5ms always        |
| Create order               | CQRS Command          | ~10-20ms             |
| Read model sync after event| Event consumption     | <100ms typically     |

### Scalability

| Component            | Scaling Strategy              |
|----------------------|-------------------------------|
| user-service         | Horizontal (stateless)        |
| order-command-service| Vertical (write throughput)   |
| order-query-service  | Horizontal (read replicas)    |
| order_read_db        | Read replicas, sharding       |
| RabbitMQ             | Clustering, multiple consumers|

### Coupling

```
Tight Coupling (BAD):
Service A → directly queries Service B's database

Loose Coupling (GOOD):
Service A → HTTP API → Service B (contract-based)
Service A → RabbitMQ Event → Service B (event-based)
```

---

## 🔌 API Reference

### API Gateway (port 8080)

| Method | Endpoint                     | Description                          |
|--------|------------------------------|--------------------------------------|
| GET    | /users/{id}/order-summary    | API Composition demo                 |
| GET    | /benchmark/compare/{userId}  | Write Count vs Read Model comparison |
| GET    | /health                      | Gateway health                       |

### User Service (port 8081)

| Method | Endpoint      | Description      |
|--------|---------------|------------------|
| GET    | /users/{id}   | Get user by ID   |
| POST   | /users        | Create user      |
| POST   | /users/seed   | Seed demo users  |

### Product Service (port 8082)

| Method | Endpoint         | Description         |
|--------|------------------|---------------------|
| GET    | /products/{id}   | Get product by ID   |
| POST   | /products        | Create product      |
| POST   | /products/seed   | Seed demo products  |

### Order Command Service (port 8083)

| Method | Endpoint                        | Description                      |
|--------|---------------------------------|----------------------------------|
| POST   | /orders                         | Create order (triggers event)    |
| GET    | /benchmark/write-count/{userId} | Benchmark: COUNT(*) on write DB  |
| POST   | /orders/seed/{userId}/{count}   | Seed N orders for benchmark      |

### Order Query Service (port 8084)

| Method | Endpoint                      | Description                     |
|--------|-------------------------------|---------------------------------|
| GET    | /order-summary/{userId}       | Get pre-computed order summary  |
| GET    | /benchmark/read-model/{userId}| Benchmark: read model lookup    |

---

## 🐳 Infrastructure

| Service    | Port | Purpose                        |
|------------|------|--------------------------------|
| mysql-main | 3306 | user_db + product_db           |
| mysql-write| 3307 | order_write_db (CQRS write)    |
| mysql-read | 3308 | order_read_db (CQRS read)      |
| rabbitmq   | 5672 | AMQP message broker            |
| rabbitmq   | 15672| Management UI (guest/guest)    |

---

## 📝 Design Decisions & Trade-offs Summary

### When to use API Composition
✅ When you need data from multiple bounded contexts  
✅ When services must remain independently deployable  
✅ When data volumes per service are reasonable  
❌ When sub-millisecond latency is critical  
❌ When all services must respond for the query to succeed  

### When to use CQRS
✅ When read and write workloads have very different scaling needs  
✅ When query-side needs denormalized/pre-aggregated data  
✅ When write-side needs complex business logic/validation  
❌ For simple CRUD applications (adds unnecessary complexity)  
❌ When strong consistency is required across read and write  

### When to use Event-Driven Read Model Updates
✅ When read model can tolerate eventual consistency  
✅ When you need to scale reads independently  
✅ When pre-aggregation dramatically improves read performance  
❌ When you need real-time exact counts (e.g., inventory)  
❌ When event ordering matters strictly  
