# Hotel Reservations Backend

Backend of the **Hotel Reservation System** for the *Software Patterns* workshop (Facade pattern).
Java 21 · Spring Boot 3.5 · Spring Security · JWT · PostgreSQL (Supabase) with JPA and Flyway.

The list of work items (readable by humans and AI agents) is in [`task.md`](task.md).

## Run it

```bash
# Local development: in-memory H2 database and an obviously fake dev-only JWT secret (data is lost on exit)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Tests
mvn test
```

Production needs environment variables (see `.env.example`):

| Variable | Required | Purpose |
|----------|----------|---------|
| `JWT_SECRET` | yes | HMAC key, at least 32 characters. The app refuses to start without it. |
| `DB_URL` | yes | JDBC URL, e.g. `jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require` |
| `DB_USERNAME` / `DB_PASSWORD` | yes | Supabase user (`postgres.<project-ref>`) and its database password |
| `CORS_ALLOWED_ORIGINS` | recommended | Frontend origin(s), comma separated |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | no | Creates an `ADMIN` account at startup |
| `PORT` | no | Defaults to 8080 (Railway sets it) |

A `Dockerfile` is included for Railway or any container host.

## API (`/api/v1`)

Everything except register / login / refresh needs `Authorization: Bearer <accessToken>`.
Errors use RFC 7807 (`application/problem+json`).

### Authentication

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register` | `{fullName, email, password}` → 201. Password: 8-72 chars, letter + digit |
| POST | `/api/v1/auth/login` | `{email, password}` → `{accessToken, refreshToken, tokenType, expiresIn}` |
| POST | `/api/v1/auth/refresh` | `{refreshToken}` → new token pair |
| GET | `/api/v1/auth/me` | Current user |

### Hotel (the route names come from the workshop statement)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/hotel/disponibilidad?checkInDate=2027-03-10&checkOutDate=2027-03-13&roomType=DOUBLE` | Free rooms with dynamic price preview (`roomType` optional) |
| POST | `/api/v1/hotel/reservar` | `{guestName, guestEmail, guestPhone?, roomType, checkInDate, checkOutDate}` → 201 |
| POST | `/api/v1/hotel/servicios/{reservationId}` | `{serviceType: SPA \| BREAKFAST \| TRANSPORT}` |
| PUT | `/api/v1/hotel/checkin/{reservationId}` | Issues the digital key (UUID) |
| PUT | `/api/v1/hotel/checkout/{reservationId}` | Revokes key, frees room, returns the final invoice |
| GET | `/api/v1/hotel/reserva/{reservationId}` | Reservation (with invoice after check-out) |

Business rules: room types `SINGLE` ($80), `DOUBLE` ($120), `SUITE` ($250) per night; high season (June, July,
December, January — configurable with `hotel.high-season-months`) multiplies by 1.5; spa $50, breakfast
$15/night, transport $30. The hotel has 18 rooms. Reservation states: `CONFIRMED → CHECKED_IN → CHECKED_OUT`.

### Quick example

```bash
curl -X POST localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"fullName":"Ana Gomez","email":"ana@example.com","password":"Passw0rdOK"}'
curl -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"ana@example.com","password":"Passw0rdOK"}'
```

## Security

- **Passwords**: BCrypt (cost 12); only the hash is stored and it is never returned.
- **JWT**: HS256, access token 15 min, refresh token 7 days. A `type` claim makes a refresh token useless as an
  access token. Signature, issuer and expiry are verified on every request.
- **Login**: same error for unknown email and wrong password, and a decoy hash comparison keeps response time similar.
- **Stateless** sessions, CSRF disabled (no cookies), CORS restricted to configured origins, unknown routes denied.
- **Ownership**: a guest can only read or modify their own reservations; `ADMIN` can access all.
- Known limits (tracked in `task.md`): refresh tokens are not revocable and there is no login rate limiting.

## Database (Supabase / PostgreSQL)

The schema is created and versioned by **Flyway** (`src/main/resources/db/migration`); Hibernate only validates it.

| Migration | Content |
|-----------|---------|
| `V1__create_schema.sql` | Tables `users`, `rooms` (18 seeded rooms), `reservations`, `reservation_rate_lines`, `reservation_services`, `invoices`, `invoice_room_lines`, `invoice_service_lines`. Portable SQL, also runs on H2. |
| `V2__postgres_overlap_and_rls.sql` | PostgreSQL only: an exclusion constraint that makes overbooking impossible even with concurrent requests, and row level security on every table so Supabase's public REST API exposes nothing. |

Design decisions:
- A room is *held* by its active reservation (`CONFIRMED` or `CHECKED_IN`); checking out frees it. There is no separate
  availability state to keep in sync.
- The price breakdown and the services are stored with the reservation, so later price changes do not alter old stays.
- `reservations.version` gives optimistic locking; concurrent modifications answer `409`.
- Use the Supabase **Session pooler** connection (the direct connection is IPv6 only).
- Invoice numbers come from the `InvoiceNumberGenerator` singleton and are resumed from the database at startup.
  This is correct for a single instance; with several instances use a database sequence.

One manual step after the first boot: Flyway holds a lock on its own history table while migrating, so enable RLS on it
once from the Supabase SQL editor:

```sql
alter table public.flyway_schema_history enable row level security;
```

## Continuous deployment

Every push to `main` runs `.github/workflows/deploy.yml`: tests, then build and push the image to ECR, restart the
container through SSM and wait until the API answers. GitHub authenticates to AWS with OIDC (role
`hotel-github-backend-deploy`, assumable only from `main` of this repository), so no AWS keys are stored in GitHub.
Repository variables used: `AWS_REGION`, `AWS_ROLE_ARN`, `SITE_URL`. Roles are created once with
`AWS_PROFILE=hotel GITHUB_OWNER=<user> ./deploy/aws/setup-github-deploy.sh`. The frontend repository deploys itself
the same way. Configuration changes (database password, CORS) still go through `deploy/aws/redeploy.sh --config-only`.

The hotel time zone (`hotel.timezone`, default `America/Bogota`) decides what "today" means for check-in validation.

## Design patterns

### Facade — `hotel/facade/HotelFacade`
The core of the workshop. The hotel has five independent subsystems (`RoomService`, `RateService`,
`AdditionalServiceService`, `BillingService`, `AccessService`). `HotelFacade` exposes one simple method per use case
and coordinates them; the controller only knows the facade.

```
HotelController ──► HotelFacade ──┬─► RoomService
                                  ├─► RateService ──► PricingStrategy
                                  ├─► AdditionalServiceService
                                  ├─► BillingService ──► InvoiceNumberGenerator (Singleton)
                                  └─► AccessService
```

For example `checkOut` revokes the key (`AccessService`), frees the room (`RoomService`) and builds the invoice
(`BillingService`) in one call. Trade-off (as in the course slides): the facade becomes a dependency of every client
and must change if the subsystems change.

### Strategy — `hotel/pricing/PricingStrategy`
`HighSeasonPricingStrategy` (×1.5) and `LowSeasonPricingStrategy` (×1) implement the same interface. `RateService`
does not contain `if (highSeason)` logic; adding a new season only needs a new strategy.

### Factory — `hotel/pricing/PricingStrategyFactory`
Registers all `PricingStrategy` beans and returns the one for a `Season`. It fails at startup if a season has no strategy.

### Singleton — `hotel/util/InvoiceNumberGenerator`
Classic thread-safe Singleton (initialization-on-demand holder idiom, private constructor, `getInstance()`), because
the hotel needs exactly one consecutive invoice sequence (`INV-2027-000001`). Additionally, every Spring `@Service`,
`@Repository` and `@Component` is a container-managed singleton by default.

### Builder — `hotel/model/Invoice.Builder`
The final invoice is immutable and assembled step by step (header, room lines, service lines); totals are computed in
the constructor and `build()` validates mandatory data.

### Repository — `*/repository`
Services depend on `RoomRepository`, `ReservationRepository`, `InvoiceRepository` and `UserRepository`, which are
Spring Data JPA interfaces. The availability rule is a single query in `RoomRepository`.

### DTO + Mapper — `hotel/dto`, `HotelMapper`
The API never exposes domain objects; records define the contract and `HotelMapper` converts to them.

## Project layout

```
com.hotel.reservations
├── auth      controller · dto · model · repository · security (JWT) · service
├── hotel     controller · facade · service (subsystems) · pricing (Strategy) · model (JPA entities) · repository · dto · util
├── config    SecurityConfig · properties classes · admin bootstrap
└── common    exception types and global handler
```

## Deploy on AWS (EC2 + ECR)

One `t3.micro` instance (free-plan eligible) runs the Docker image pulled from ECR. The `JWT_SECRET` is generated
and stored in SSM Parameter Store (`/hotel/jwt-secret`), never in the repository. Port 8080 is open only to
CloudFront; the frontend repo creates the distribution that exposes the API under `/api/*` over HTTPS.

```bash
# Create ECR repo, push the image, create the role, security group and instance (re-runnable)
AWS_PROFILE=hotel ./deploy/aws/provision.sh

# After changing the code: rebuild, push and restart the container through SSM
AWS_PROFILE=hotel ./deploy/aws/redeploy.sh

# Change only the allowed frontend origin
AWS_PROFILE=hotel CORS_ALLOWED_ORIGINS=https://<distribution>.cloudfront.net ./deploy/aws/redeploy.sh --config-only
```

Settings live in SSM Parameter Store under `/hotel/env/*` (each one becomes an environment variable of the
container). To set the database connection, copy `deploy/aws/secrets.env.example` to `deploy/aws/secrets.env`
(ignored by git), write the database password in it and run `redeploy.sh`.
