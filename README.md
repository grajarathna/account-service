# Account Service

Spring Boot REST API for managing savings accounts with nickname validation and customer account limits.

## What it does

- Creates savings accounts with optional nicknames (5-30 chars)
- Gets account details by account number
- Checks nicknames for profanity using external API
- Enforces max 5 accounts per customer
- Uses Redis caching for reads
- Handles errors gracefully
- Manages DB schema with Flyway

## Tech stack

- Java 17
- Spring Boot 4.1.0
- Spring Data JPA (Hibernate)
- PostgreSQL 16.2
- Redis 7.2
- Flyway (DB migrations)
- JUnit 5 & Mockito
- Gradle 9.5.1

## Setup

You'll need:
- Java 17+
- PostgreSQL 16+ on `localhost:5433`
- Redis 7+ on `localhost:6380`
- Profanity API key (sent via email)

### Quick Docker setup

Start PostgreSQL:
```bash
docker run -d \
  --name postgres \
  --restart always \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin \
  -e POSTGRES_DB=accountdb \
  -v postgres:/var/lib/postgresql/data \
  -p 5433:5432 \
  postgres:16.2 \
  postgres -c wal_level=logical
```

Start Redis:
```bash
docker run -d \
  --name redis \
  --restart always \
  -v redis_data:/data \
  -p 6380:6379 \
  redis:7.2 \
  redis-server --requirepass admin --appendonly yes
```

## How to run the app

```bash
PROFANITY_API_KEY=<api-key-provided-in-email> ./gradlew bootRun
```

App starts on `http://localhost:8080`

## API Usage

### Create Account
```bash
POST /api/v1/accounts
Content-Type: application/json

{
  "customerName": "John Doe",
  "accountNickname": "MySavings"  // Optional, 5-30 characters
}
```

**Response:**
```json
{
  "customerId": "uuid",
  "accountNumber": "020-1222-1234567-000",
  "customerName": "John Doe",
  "accountNickname": "MySavings"
}
```

**Try it:**
```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "accountNickname": "MySavings"
  }'
```

### Get Account
```bash
GET /api/v1/accounts/{accountNumber}
```

**Try it:**
```bash
curl http://localhost:8080/api/v1/accounts/020-1222-1234567-000
```

### Create Additional Account
```bash
POST /api/v1/accounts/{customerId}
Content-Type: application/json

{
  "accountNickname": "EmergencyFund"  // Optional, 5-30 characters
}
```

**Try it:**
```bash
curl -X POST http://localhost:8080/api/v1/accounts/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -d '{
    "accountNickname": "Emergency"
  }'
```

### Get All Customer Accounts
```bash
GET /api/v1/accounts/customer/{customerId}
```

## How it works

**Architecture:**
- Controller layer: REST endpoints + validation
- Service layer: business logic (profanity check, limits, account numbers)
- Repository layer: JPA/Hibernate DB access
- Exception handling: centralized with `@RestControllerAdvice`

### Why two account creation endpoints?

The spec says "Creates a new savings bank account", but to support the 5-account limit per customer properly, I added two endpoints:
- `POST /api/v1/accounts` - first account for new customer
- `POST /api/v1/accounts/{customerId}` - additional accounts for existing customer

Why this matters:
- Tracks customer ID across accounts
- Can enforce 5-account limit
- Account numbers share same base per customer (NZ banking pattern I've seen: `020-1222-1234567-000`, `020-1222-1234567-001`)

### Why GET all customer accounts?

Added `GET /api/v1/accounts/customer/{customerId}` for:
- Verifying account creation worked
- Listing customer's accounts
- Testing the 5-account limit

### Key implementation details

**Account numbers:**
- Format: `020-1222-{7-digit-unique}-{3-digit-suffix}`
- Random unique number with collision retry (max 5 tries)
- Suffix increments per customer (000, 001, 002...)
- TODO: switch to DB sequence for guaranteed uniqueness

**Profanity filter:**
- Calls external API via `ProfanityClient`
- Only runs if nickname provided and not blank
- 400 if profanity found
- 503 if API unreachable

**Account limits:**
- Max 5 accounts per customer at service layer
- `synchronized` method for creating additional accounts (works for single instance only)
- TODO: need distributed lock (Redis/Redisson) for multi-instance

**Caching:**
- Redis cache for account lookups (`accounts` cache)
- Redis cache for customer account lists (`customerAccounts` cache)
- Cache cleared when creating additional accounts
- 10 min TTL

**Database:**
- Single `account` table, UUID primary key
- Unique constraint on `account_number`
- Check constraint on `account_nickname` (5-30 chars)
- Index on `customer_id`
- Flyway for migrations

### Errors

| Status | Scenario |
|--------|----------|
| 400 Bad Request | Validation errors, profane nickname, 5-account limit exceeded |
| 404 Not Found | Account or customer not found |
| 503 Service Unavailable | Database or profanity API unavailable |

## Testing

Run all tests:
```bash
./gradlew test
```

**Test coverage:**
- 6 unit tests for `AccountService`
- 4 controller tests for `AccountController`
- Integration test disabled (needs infrastructure)

## What's left to do

### High priority
1. **Logging**: Add logs at critical points (account creation, errors, API calls) for observability
2. **Account numbers**: Use DB sequence instead of random generation
3. **Distributed locking**: Redis/Redisson locks for multi-instance deployments
4. **DB error handling**: Better handling when DB unavailable (currently just 503)
5. **More tests**: Edge cases like missing nickname validation, concurrent creation, cache eviction
6. **Customer identity**: Current design allows duplicate customers with same name. Real solution needs separate customer registration with unique identifier (email, phone, national ID) before account creation

### Medium priority
7. **Code quality**: Integrate static analysis (SonarQube, Checkstyle, SpotBugs) and formatting tools (Spotless) for consistent code standards
8. **Branch codes**: Accept from request instead of hardcoding
9. **Rate limiting**: Prevent abuse
10. **Monitoring**: Metrics, health checks, tracing
11. **Structured logging**: JSON logs with correlation IDs
12. **Integration tests**: TestContainers with PostgreSQL + Redis
13. **E2E tests**: Critical user journeys
14. **Resilience**: Retry for reads, circuit breaker for external APIs (no retries for writes - creates dupes)
15. **API docs**: OpenAPI/Swagger

### Low priority
16. **DTO optimization**: Dedicated DTO for customer account lists (avoid duplication)
17. **Name index**: GIN index on `customer_name` if filtering by name added
18. **Audit trail**: Track who/when for account operations
19. **Optimistic locking**: `@Version` column for when account updates supported

**Testing note:** Following test pyramid - unit tests first (fast, isolated, many). Integration/E2E tests deferred for time-limited assessment since unit tests give best ROI for validating core logic.

## Security TODOs

**Note:** Focus here was functional requirements. Security hardening needed before real use - shift-left security principles.

### High priority
1. **Auth**: Spring Security with JWT/OAuth2. Role-based access so customers only see their own accounts
2. **Secrets**: Move to Vault/AWS Secrets Manager/Azure Key Vault with rotation
3. **API security**: Rate limiting (Bucket4j/Redis), CORS config, API key validation

### Medium priority
4. **PII protection**: Mask sensitive fields in logs (names, account numbers). Maybe field-level encryption
5. **Audit logging**: Security trail for account creation, access, auth failures, authz violations


