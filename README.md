# Microservices-assessment-vishal-shukla

Two Spring Boot services for the LoadUp assessment:

- `order-service`
- `notification-service`

The code is organized as a monorepo so it can be cloned once and run locally with Docker.

## Overview

The system uses an outbox-driven workflow:

1. A client calls `order-service` to create or update an order.
2. `order-service` persists the order in the tenant database and writes an outbox record in the same transaction.
3. The scheduled outbox publisher reads unpublished rows and publishes `OrderEvent` messages to Kafka.
4. `notification-service` consumes those events, resolves the tenant, and stores notifications in the matching tenant database.

Both services use tenant-aware routing with the `X-Tenant-Id` header.

## Assumptions And Dependencies

- Java 21 or higher
- Maven 3.9+
- Docker and Docker Compose
- PostgreSQL
- Kafka
- Spring Boot 3.4.x
- The services expect a valid `X-Tenant-Id` header on tenant-scoped endpoints
- The two supported tenants are `tenant-a` and `tenant-b`
- Each tenant has its own PostgreSQL database
- Each service owns its own schema inside each tenant database

## How To Run

### Run Tests

From the monorepo root:

```bash
mvn -q test
```

This runs the unit tests for all modules. The notification service also contains an end-to-end integration test that uses Testcontainers for PostgreSQL and Kafka.

### Run The Full System

After cloning the repo, change into the repository directory and start the stack:

```bash
git clone git@github.com:heyvishy/microservices-assessment-vishal-shukla.git
cd microservices-assessment-vishal-shukla
docker compose up --build
```

If you want a clean restart, stop the stack and remove volumes first:

```bash
docker compose down -v
docker compose up --build
```

Because the compose file does not pin container names, repeated runs will not collide with older containers from the same project.

## Troubleshooting

If startup fails on a fresh machine, check these first:

- `docker compose` says a port is already in use:
  - Stop the process using that port or change the port mapping in `docker-compose.yml`
  - Common ports used here are `5432`, `29092`, `8081`, and `8082`
- You see leftover Docker containers or volumes from an earlier run:
  - Run `docker compose down -v` from the repository root
  - If needed, remove stale containers manually with `docker ps -a`
- The services start but cannot connect to Postgres or Kafka:
  - Make sure you are running `docker compose up --build` from the repository root
  - Confirm Docker Desktop is running and healthy
- You cloned the repo into a parent folder and started Compose from the wrong directory:
  - `cd` into the cloned repository directory first, then run `docker compose up --build`

## API Endpoints

All tenant-aware HTTP endpoints require:

```http
X-Tenant-Id: tenant-a
```

or

```http
X-Tenant-Id: tenant-b
```

### Order Service

Base URL: `http://localhost:8081`

#### Create Order

- `POST /api/v1/orders`

Example:

```bash
curl -X POST "http://localhost:8081/api/v1/orders" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-a" \
  -d '{
    "customerId": "CUST-1001",
    "customerEmail": "customer@example.com",
    "description": "First order",
    "totalAmount": 99.95,
    "currency": "USD"
  }'
```

#### Get Order

- `GET /api/v1/orders/{orderId}`

Example:

```bash
curl "http://localhost:8081/api/v1/orders/7d0f8c8e-5e0f-4d1b-a9c5-9ec6f7fdc1a2" \
  -H "X-Tenant-Id: tenant-a"
```

#### List Orders

- `GET /api/v1/orders`

Example:

```bash
curl "http://localhost:8081/api/v1/orders" \
  -H "X-Tenant-Id: tenant-a"
```

#### Update Order

- `PUT /api/v1/orders/{orderId}`

Example:

```bash
curl -X PUT "http://localhost:8081/api/v1/orders/7d0f8c8e-5e0f-4d1b-a9c5-9ec6f7fdc1a2" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-a" \
  -d '{
    "status": "PROCESSING",
    "description": "Updated description"
  }'
```

#### Cancel Order

- `POST /api/v1/orders/{orderId}/cancel`

Example:

```bash
curl -X POST "http://localhost:8081/api/v1/orders/7d0f8c8e-5e0f-4d1b-a9c5-9ec6f7fdc1a2/cancel" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-a" \
  -d '{
    "reason": "Customer requested cancellation"
  }'
```

### Notification Service

Base URL: `http://localhost:8082`

#### List Notifications

- `GET /api/v1/notifications`

Example:

```bash
curl "http://localhost:8082/api/v1/notifications" \
  -H "X-Tenant-Id: tenant-a"
```

## Design Decisions And Tradeoffs

- A monorepo keeps the submission easy to clone, build, and run from one GitHub URL.
- Shared Kafka event types live inside the same repository so the build does not depend on a separate published artifact.
- The system uses an outbox pattern to avoid losing events between database writes and Kafka publishing.
- Tenant routing is done in the application layer, which keeps the services self-contained and easy to run locally.
- The API uses explicit tenant headers so tenant behavior is visible during testing and demos.
- The tradeoff of a monorepo is tighter coupling between modules, but it is the simplest and most reliable shape for a submission that must run on another machine.

## Notes

- Service-specific build notes live in the module directories, but the root README is the source of truth for running the whole system.
- If you are evaluating the app manually, start with `order-service`, then check Kafka-backed notification creation through `notification-service`.
