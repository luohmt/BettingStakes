# Betting Stakes Service

A lightweight betting backend powered by Java's built-in HttpServer. It provides session management, stake submission, and high-stake leaderboard queries using concurrency-friendly data structures for high performance and thread safety.

## Features

- Session management: create or retrieve a session key for a customer
- Stake submission: track each customer's maximum stake per bet offer
- Leaderboard: return the top 20 highest stakes per bet offer as customerId=stake
- Minimal routing: custom annotation-based router and param resolvers (no web frameworks)
- Unified exceptions: `BettingException` with error codes and HTTP status
- Custom logging: `com.betting.util.Logger` with `info/warn/error`

## Architecture

```
src/main/java/com/betting/
├── api/controller/              # Session and stake controllers
├── api/exception/               # BettingException and error codes
├── core/model/                  # Domain model (Session)
├── core/service/                # Service interfaces and implementations
├── infrastructure/config/       # App configuration (port, thread pool)
├── infrastructure/http/         # Router, resolvers, interceptors
└── util/                        # Logger, validator, light JSON
```

Highlights:
- The stake service uses `ConcurrentHashMap` and `ConcurrentSkipListMap` to maintain per-offer max stakes and a descending leaderboard, safely under concurrency.
- The router uses `@Route` annotations and supports Path/Query/Body parameter resolution and request/response/exception interceptors.

## Solution rationale

- Keep it simple: no external web or logging frameworks; rely on Java HttpServer, small router, and a tiny `Logger`.
- Correctness over complexity: session validation is only for authorization at submit time; stakes are stored independently by `customerId` and `betOfferId`, so expired sessions never remove historical stakes.
- Concurrency model: per-offer `ConcurrentSkipListMap<Integer, Set<Integer>>` keeps stakes in descending order; updates remove from old bucket and add to new in a few atomic map ops; reads stream from the head to take top 20.
- Predictable performance: O(log n) insert/update (skip list), near O(1) top-20 retrieval by early termination.
- Failure handling: `BettingException` carries error code and HTTP status; router maps exceptions centrally; interceptor logs failures consistently.
- Operational simplicity: configuration via system properties; single shaded JAR; custom logger writes to stdout/stderr.
- Trade-offs: no persistence—data is in-memory; no security framework—session is a simple token; router is minimal—no advanced features like filters or DI.
- Future extensions: plug a persistence layer, add rate limiting as an interceptor, and expose health/metrics endpoints.

## Requirements

- Java 21+
- Maven 3.6+

## Build and Run

1) Compile
```bash
mvn clean compile
```

2) Test
```bash
mvn test
```

3) Start
```bash
# PowerShell/cmd (no quotes around the main class)
mvn compile exec:java -Dexec.mainClass=com.betting.BettingApplication

# Or use the shaded JAR (recommended for running)
mvn -q clean package
java -jar BettingStakes-1.0-SNAPSHOT.jar
```

Note: In PowerShell, quoting the main class like "com.betting.BettingApplication" causes ClassNotFoundException.

Default port: 8001.

## Configuration

Supported via JVM properties or environment variables:

JVM properties:
```bash
-Dbetting.port=8001
-Dbetting.thread.pool.size=50
```


## API

All endpoints return plain text (no JSON wrapper).

1) Create/Get session
```
GET /{customerId}/session
```
Response: session key string

Example:
```bash
curl http://localhost:8001/123/session
```

2) Submit stake
```
POST /{betOfferId}/stake?sessionkey={sessionKey}
Content-Type: application/json

{
  "stake": 100
}
```
Behavior: updates the max stake for this customer and bet offer only if the new stake is higher. Returns an empty string on success.

Example:
```bash
curl -X POST "http://localhost:8001/77/stake?sessionkey=abc123" \
  -H "Content-Type: application/json" \
  -d '{"stake": 200}'
```

3) Get top 20 high stakes
```
GET /{betOfferId}/highstakes
```
Response: comma-separated `customerId=stake` entries, for example:
```
42=800,17=700,9=650
```

## Errors and Error Codes

When `BettingException` is thrown, the router maps it to the appropriate HTTP status and message. Error codes are defined in `com.betting.api.exception.BettingException.ErrorCode`:

- Session: `INVALID_SESSION(403)`, `SESSION_EXPIRED(403)`, `SESSION_NOT_FOUND(404)`
- Stake: `INVALID_STAKE_AMOUNT(400)`, `INVALID_BET_OFFER_ID(400)`, `STAKE_TOO_LOW(400)`, `STAKE_TOO_HIGH(400)`
- Customer: `INVALID_CUSTOMER_ID(400)`, `CUSTOMER_NOT_FOUND(404)`
- System: `INTERNAL_ERROR(500)`, `SERVICE_UNAVAILABLE(503)`, `RATE_LIMIT_EXCEEDED(429)`
- Validation: `MISSING_PARAMETER(400)`, `INVALID_PARAMETER_FORMAT(400)`

Common business validations are centralized in `com.betting.util.BettingValidator` and invoked at controller entry points.

## Logging

All logging uses the custom `com.betting.util.Logger`:
- `Logger.info(String, Object...)`
- `Logger.warn(String, Object...)`
- `Logger.error(String, Throwable, Object...)`

Log format: `[LEVEL] yyyy-MM-dd HH:mm:ss.SSS - message`. No external logging framework is used.

## Concurrency and Performance

- Submit stake: skip list insertion ~O(log n); maintains descending keys (stake) to customer sets
- Leaderboard: stream the descending map and take the first 20 entries, avoiding full sorts
- Core containers: `ConcurrentHashMap`, `ConcurrentSkipListMap`, concurrent sets

## Key Classes

- `com.betting.BettingApplication`: app entry point; starts HttpServer, registers router and interceptors
- `com.betting.infrastructure.http.router.Router`: annotations, param resolution, interceptor chain, exception handling
- `com.betting.infrastructure.http.interceptor.LoggingInterceptor`: request/response/exception logging
- `com.betting.api.controller.SessionController`: session creation/retrieval
- `com.betting.api.controller.StakeController`: stake submission and leaderboard
- `com.betting.core.service.impl.StakeServiceImpl`: concurrent stake and leaderboard maintenance
- `com.betting.infrastructure.config.BettingConfig`: reads port and thread pool size
- `com.betting.util.Logger`: application logging; `com.betting.util.BettingValidator`: input/business validation
