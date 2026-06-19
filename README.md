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

## Architecture

### System Design

- `order-service` owns order writes and outbox creation.
- `notification-service` owns event consumption and notification persistence.
- The services communicate asynchronously through Kafka, which avoids synchronous coupling and keeps order creation fast.
- The shared event contract lives in the same repo for this submission so the build stays clone-and-run friendly without a separate artifact repository.

### Multi-Tenancy Design

- Tenant isolation is a platform principle here, not just a database column.
- Each tenant gets its own PostgreSQL database, and each service owns its own schema inside that tenant database.
- HTTP requests are routed through tenant-aware filters, and the application routes to the correct data source before any persistence call happens.
- This keeps tenant separation consistent across API, service, and database layers.

### Tenant Context Propagation

- Inbound requests carry `X-Tenant-Id`.
- The tenant filter validates the header and stores the tenant in request context and MDC.
- Service methods require tenant context before executing business logic.
- The routing data source resolves the target database from that context.
- Kafka events carry the tenant ID so the consumer can restore context before writing to the database.

### Resilience And Failure Handling

- The outbox pattern prevents order writes from being lost if Kafka publishing fails.
- The notification consumer is idempotent, so duplicate event delivery does not create duplicate notifications.
- Missing or unknown tenant requests fail fast with clear `400` responses.
- Not-found and conflict cases return explicit API errors rather than generic server failures.
- If a service or dependency is unavailable, the system prefers consistency and retryable recovery over silent data loss.

### Industry Standards Applied

- Outbox pattern: chosen to keep database writes and event publishing consistent.
- Idempotent consumer: chosen to tolerate Kafka retries and duplicate delivery.
- Schema-per-service inside tenant databases: chosen to keep service ownership clear while still isolating tenants.
- `ProblemDetail` responses: chosen for predictable, standard error payloads.
- Structured logging with trace and tenant context: chosen to make production debugging practical.
- Testcontainers integration tests: chosen to verify the system against real Postgres and Kafka behavior.

### Production Readiness

- Add authn/authz so callers cannot choose arbitrary tenants.
- Add metrics and alerts for outbox lag, publish failures, and consume failures.
- Add stronger retry and dead-letter handling for Kafka and database outages.
- Add readiness and liveness tuning plus rollout safeguards for deployment.
- Add backup, restore, and retention policies before real production use.

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
