# task.md — Hotel Reservations Backend (Facade Pattern Workshop)

> Machine-readable task board for AI agents and humans. Update the checkbox and the **Status** line
> when you finish a task. Do not mark a task done unless `mvn test` passes.
> Source of truth for the requirements: `Patrón Facade (1).pdf` (slides 17-20).

## 0. Project context

| Item | Value |
|------|-------|
| Project | Hotel reservation system — **backend only** (frontend lives in another repo) |
| Stack | Java 21, Spring Boot 3.5.13, Maven, Spring Security, JJWT 0.12 |
| Base path | `/api/v1` |
| State | PostgreSQL on Supabase (JPA + Flyway). H2 in memory for the `dev` profile and tests |
| Language rule | **All code, comments, JSON fields and docs identifiers in English** (only the 6 route names inherited from the workshop are Spanish) |
| Build / test | `mvn test` · run locally with `mvn spring-boot:run -Dspring-boot.run.profiles=dev` |
| Required env in prod | `JWT_SECRET` (>= 32 chars), `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; optional `CORS_ALLOWED_ORIGINS`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `PORT` |

## 1. Architecture rules (do not break)

1. Controllers call **only** `HotelFacade` (hotel) or `AuthService` (auth). Never a subsystem service.
2. Subsystem services (`RoomService`, `RateService`, `AdditionalServiceService`, `BillingService`, `AccessService`) do **not** know each other; only the facade orchestrates them.
3. Persistence goes through Spring Data repository interfaces (`*Repository`). The schema changes only through new Flyway migrations (never edit an applied one); Hibernate runs in `validate` mode.
4. Money is always `BigDecimal` with scale 2. Never `double`.
5. Errors are thrown as exceptions from `common.exception` and rendered by `GlobalExceptionHandler` as RFC 7807 problem JSON.
6. Secrets never in source control. Passwords only stored as BCrypt hashes.

## 2. Tasks

Legend: `[x]` done · `[ ]` pending

### 2.1 Subsystems (slide 18)

- [x] **T-01 RoomService** — availability by dates and type (SINGLE, DOUBLE, SUITE), reserve and release rooms. ≥ 15 predefined rooms (18 seeded: 6 single, 8 double, 4 suite). Thread-safe booking via `Room.book`.
- [x] **T-02 RateService (TarifaService)** — total price by season (high = base × 1.5, low = base), nights and room type. Priced night by night, so stays crossing seasons are exact. Uses Strategy pattern.
- [x] **T-03 AdditionalServiceService** — extras: spa $50, breakfast $15/night, transport $30.
- [x] **T-04 BillingService** — itemized invoice (room lines per season + service lines + subtotals + total), unique consecutive number.
- [x] **T-05 AccessService** — UUID digital key, issued at check-in, revoked at check-out.

### 2.2 Facade (slide 18)

- [x] **T-06 HotelFacade** — `createReservation`, `addService`, `checkIn`, `checkOut` (computes extra charges and returns final invoice), plus `checkAvailability` and `getReservation`.
- [x] **T-07 Reservation life cycle** — `CONFIRMED → CHECKED_IN → CHECKED_OUT`; invalid transitions return 409.

### 2.3 REST endpoints (slide 18, prefixed with `/api/v1`)

- [x] **T-08** `POST /api/v1/hotel/reservar`
- [x] **T-09** `GET  /api/v1/hotel/disponibilidad?checkInDate&checkOutDate[&roomType]` (returns price preview per room)
- [x] **T-10** `POST /api/v1/hotel/servicios/{reservationId}`
- [x] **T-11** `PUT  /api/v1/hotel/checkin/{reservationId}`
- [x] **T-12** `PUT  /api/v1/hotel/checkout/{reservationId}`
- [x] **T-13** `GET  /api/v1/hotel/reserva/{reservationId}`

### 2.4 Authentication and security (extra, requested by the student)

- [x] **T-14 Register** `POST /api/v1/auth/register` — validation, password policy (8-72 chars, letter + digit), duplicate email → 409.
- [x] **T-15 Login** `POST /api/v1/auth/login` — BCrypt (cost 12) verification, generic error message, decoy hash to avoid user enumeration by timing.
- [x] **T-16 JWT** — HS256, access token 15 min, refresh token 7 days, `type` claim prevents using a refresh token as access token.
- [x] **T-17 Refresh / Me** `POST /api/v1/auth/refresh`, `GET /api/v1/auth/me`.
- [x] **T-18 Stateless security chain** — all `/api/v1/**` authenticated except register/login/refresh; JSON 401/403; CORS from env; everything else denied.
- [x] **T-19 Ownership** — a guest can only see/modify their own reservations; `ADMIN` can access all.
- [x] **T-20 Optional admin bootstrap** — created only when `ADMIN_EMAIL` and `ADMIN_PASSWORD` are set (no default credentials).

### 2.5 Quality and documentation

- [x] **T-21 Tests** — `RateServiceTest` (pricing + Singleton), `HotelFlowIntegrationTest` (auth rules + full lifecycle + overbooking + ownership).
- [x] **T-22 README** — setup, endpoints, security notes and documented design patterns.
- [x] **T-23 Dockerfile** for Railway/any container host.

### 2.5.1 Database (Supabase)

- [x] **T-31 JPA entities + Flyway** — `V1` schema with seed rooms, `V2` PostgreSQL overlap constraint and RLS.
- [x] **T-32 Concurrency safety** — exclusion constraint against overbooking, optimistic locking on reservations.
- [x] **T-33 Configuration in SSM** — `/hotel/env/*` parameters become the container environment.
- [ ] **T-34 First production boot against Supabase** — needs the database password in `deploy/aws/secrets.env`, then `./deploy/aws/redeploy.sh`.
- [ ] **T-35 Review Supabase advisors** after the first boot (security: RLS on all tables incl. `flyway_schema_history`).

### 2.6 Pending / next steps (not done yet)

- [ ] **T-24 Deploy backend to PROD** (Railway or similar) — set `JWT_SECRET` and `CORS_ALLOWED_ORIGINS` (Vercel URL). *Required by the workshop (note 2), needs the student's account.*
- [ ] **T-25 Frontend** (separate repo, Vercel) — see slide 19: room search, price preview, booking form, management panel, final invoice view. Consume this API.
- [ ] **T-26 Refresh token revocation / rotation** — currently stateless; a stolen refresh token stays valid until it expires.
- [ ] **T-27 Login rate limiting** (e.g. Bucket4j) against brute force.
- [ ] **T-28 Cancel reservation** endpoint (`DELETE`/`PUT cancel`) using `RoomService.release`.
- [ ] **T-29 OpenAPI/Swagger** documentation.

## 3. Definition of done for any new task

1. Code in English, follows the architecture rules in section 1.
2. A test covers the new behavior; `mvn test` is green.
3. README endpoint table / patterns section updated if the public surface changed.
4. This file updated (checkbox + new task entry if scope grew).
