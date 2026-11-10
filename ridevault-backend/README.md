# 🚀 RideVault Backend — Spring Boot 4 / Spring Framework 7 REST API

> **Dynamic ride-booking platform with stateless JWT security, raw SQL optimistic concurrency control, 4-digit boarding Start PIN verification, and `@Transactional` wallet settlements.**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Framework](https://img.shields.io/badge/Spring%20Framework-7.0-brightgreen.svg)](https://spring.io/projects/spring-framework)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-7.1%20JWT-blue.svg)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%2B-blue.svg)](https://www.postgresql.org/)

---

## 🎯 Core Engineering Architecture

| Feature | Implementation Details | Key Classes & Files |
| :--- | :--- | :--- |
| **Layered REST API** | Resource-oriented `@RestController`s, Jakarta `@Valid` DTOs, standardized `ApiResponse<T>`, `Pageable` pagination, and centralized error responses. | `controller.*`, `dto.*`, `GlobalExceptionHandler.java` |
| **Spring Security & JWT** | BCrypt password hashing, stateless JWT access tokens, SHA-256 Refresh Token Rotation (`/refresh`, `/logout`), RBAC, and participant ownership checks. | `SecurityConfig.java`, `JwtService.java`, `JwtAuthenticationFilter.java` |
| **Ride State Machine & Start PIN** | Strictly enforced lifecycle (`PENDING → ACCEPTED → ARRIVED → IN_PROGRESS → COMPLETED` / `CANCELLED`) with 4-digit boarding Start PIN verification & immutable logs. | `RideStateMachine.java`, `RideService.java`, `RideStatusLog.java` |
| **Optimistic Concurrency & Auto-Expiry** | Atomic SQL conditional updates (`WHERE status = 'PENDING'`) eliminate double-booking races and guard `@Scheduled` 3-minute ride auto-expiry. | `RideRepository.acceptRideOptimisticallyRawSql()`, `RideExpiryScheduler.java` |
| **Atomic Wallet Settlement** | `@Transactional` 80/20 driver/platform splits with `PESSIMISTIC_WRITE` row locks (`SELECT ... FOR UPDATE`) and double-entry `wallet_transactions`. | `WalletSettlementService.java`, `WalletRepository.java` |
| **14-Table BCNF PostgreSQL** | Strictly normalized relational schema including `ride_ratings` and geographic `surge_zones`, initialized cleanly with idempotent DDL. | `../database/DDL.sql`, `src/main/resources/schema.sql` |

---

## 🔒 High-Concurrency Optimistic Locking (`WHERE status='PENDING'`)

When multiple drivers attempt to accept the same pending ride simultaneously, PostgreSQL row-level locking executes the conditional update atomically:

```sql
UPDATE rides
SET driver_id = :driverId,
    vehicle_id = :vehicleId,
    status = 'ACCEPTED',
    accepted_at = CURRENT_TIMESTAMP
WHERE ride_id = :rideId
  AND status = 'PENDING';
```

* **First driver wins:** Returns `rowsUpdated = 1` and transitions the ride to `ACCEPTED`.
* **Competing drivers rejected cleanly:** Return `rowsUpdated = 0`, raising `RideAlreadyClaimedException` (`409 Conflict`).
* **Scheduler safety:** `RideExpiryScheduler` uses the exact same `WHERE status = 'PENDING'` conditional update so background expiry can never overwrite a ride that was just accepted.

---

## 💰 Atomic Wallet Settlements

* **80% / 20% Revenue Split:** Drivers earn 80% of the fare; the platform records 20% commission on completed rides.
* **Pessimistic Row Locking:** `walletRepository.findByDriverIdWithLock()` prevents lost updates under concurrent transactions.
* **Zero Drift Guarantee:** Any runtime error inside `@Transactional(rollbackFor = Exception.class)` rolls back both wallet balance updates and ledger entries together.

---

## 🚀 Running the Application

### 1. Database Setup
Start PostgreSQL using Docker Compose from the repo root (creates `ridevault` and applies `../database/DDL.sql` + `../database/DataInsertion.sql`):

```bash
docker compose up -d
```

Or on a local PostgreSQL server:
```sql
CREATE DATABASE ridevault;
```

Copy `src/main/resources/application-local.yml.example` to `src/main/resources/application-local.yml` (gitignored) and enter your PostgreSQL credentials.

### 2. Build & Run

```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

The server starts on `http://localhost:8080`.

### 3. Run Integration Tests

```bash
mvn test
mvn test -Dtest=RideVaultSpringBoot4SmokeTest
mvn test -Dtest=RideConcurrencyTest
mvn test -Dtest=WalletSettlementRollbackTest
```

---

## 🌐 Built-in Web Portals & API Docs

* **Live Operations Hub:** `http://localhost:8080/` (real-time PostgreSQL metrics & Gandhinagar surge zones)
* **Rider Portal:** `http://localhost:8080/rider.html` (BCrypt login, Haversine fare estimation, SOS emergency contact, Start PIN display, 5★ driver rating, audit timeline, paginated history)
* **Driver Portal:** `http://localhost:8080/driver.html` (Online/Offline toggle, optimistic ride claiming, Start PIN entry, 80% payouts, paginated wallet ledger)
* **Swagger / OpenAPI UI:** `http://localhost:8080/swagger-ui.html`
* **Postman Collection:** `RideVault.postman_collection.json`

---

## 📡 REST API Reference

### Authentication (`/api/v1/auth`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/register/rider` | Public | Register passenger account and issue JWT tokens |
| `POST` | `/register/driver` | Public | Register driver, vehicle, insurance document, and wallet |
| `POST` | `/login` | Public | Authenticate via BCrypt and issue JWT access + refresh tokens |
| `POST` | `/refresh` | Public | Rotate refresh token and return fresh token pair |
| `POST` | `/logout` | Authenticated | Revoke all active refresh tokens for the user |

### Rides (`/api/v1/rides`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/estimate-fares` | Authenticated | Compute Haversine distance, geographic surge multiplier, and category fares |
| `POST` | `/book` | Rider | Request a ride (`PENDING`) and generate a 4-digit Start PIN |
| `GET` | `/available` | Driver | List category-matched pending rides (returns empty if driver is Offline) |
| `PUT` | `/{rideId}/accept` | Driver | Claim ride atomically (`WHERE status='PENDING'`) |
| `PUT` | `/{rideId}/status` | Rider / Driver | Transition state (`ARRIVED`, `IN_PROGRESS` with `startPin`, `CANCELLED`) |
| `PUT` | `/{rideId}/complete` | Driver | Finalize trip and credit 80% payout to driver wallet |
| `POST` | `/{rideId}/rate` | Rider | Submit 1–5 star rating and atomically update `drivers.avg_rating` |
| `GET` | `/{rideId}/timeline` | Participant / Admin | View immutable chronological `ride_status_logs` |
| `GET` | `/{rideId}` | Participant / Admin | Fetch ride details (Start PIN masked from drivers) |

### Profiles, Wallet & Telemetry
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` / `PUT` | `/api/v1/rider/me` | Rider | View profile / update SOS emergency contact |
| `GET` | `/api/v1/rider/active-ride`, `/rides/paged` | Rider | Restore active ride & query paginated/filtered history |
| `GET` | `/api/v1/driver/me`, `/active-ride`, `/rides/paged` | Driver | Driver profile, active trip, paginated history |
| `PUT` | `/api/v1/driver/availability` | Driver | Toggle Online / Offline availability |
| `GET` | `/api/v1/wallet/me`, `/transactions` | Driver | Balance & paginated `CREDIT`/`DEBIT` transaction ledger |
| `POST` | `/api/v1/wallet/withdraw` | Driver | Atomic wallet withdrawal with `PESSIMISTIC_WRITE` lock |
| `GET` | `/api/v1/hub/overview` | Public | Aggregated live platform counts & surge zones |
| `GET` / `PUT` | `/api/v1/admin/**` | Admin | Manage surge zone multipliers and driver verification |
