# Repository Guide

## Project

- This is a Java 17 / Spring Boot 4.1.1 service. `com.example.demo.DemoApplication` is the application entrypoint; keep Spring-managed code beneath `com.example.demo` so component scanning finds it.
- Use the Maven Wrapper: `./mvnw` on Unix-like shells or `.\mvnw.cmd` in PowerShell. It pins Maven 3.9.16.
- Lombok annotation processing is explicitly configured for both main and test compilation in `pom.xml`; retain both compiler-plugin executions when changing the build.
- `ModelMapperConfig` supplies the application-wide `ModelMapper` bean; inject it rather than constructing mappers in application code.

## Authentication

- Authentication uses an HS-256 JWT in the `ACCESS_TOKEN` HTTP-only, `SameSite=Strict` cookie. Its UUID `sessionId` must match `UserEntity.activeSessionId`, so a new login and logout revoke older cookies.
- `DataInitializer` seeds the `roles` table from `Role`: `ADMIN`, `GERENTE`, `CONTADOR`, and `EJECUTIVO`. Users and roles use UUID primary keys; do not replace the `RoleEntity` relationship with an unpersisted enum field.
- All `/api/v1/users/**` endpoints require `ADMIN`; preserve both the route-level and controller-level checks. Usernames are normalized to lowercase before the unique lookup and save.
- `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, and protected `GET /api/v1/auth/me` manage and read the JWT cookie. API bodies use `ApiResponse<T>`, including validation and `401`/`403` errors.
- Request DTOs belong in `dto/request`, response DTOs in `dto/response`, and API exception handling in `exception/GlobalExceptionHandler`.
- Use `@ValidPassword` for password-bearing request fields; `PasswordValidator` is also the source of truth for the bootstrap-admin password check.
- Set `APP_BOOTSTRAP_ADMIN_USERNAME` and `APP_BOOTSTRAP_ADMIN_PASSWORD` to create the first `ADMIN` on startup. The bootstrap never overwrites an existing username.

## Commands

- Run all tests: `.\mvnw.cmd test` (PowerShell) or `./mvnw test` (Unix-like shells).
- Run one security flow test: `.\mvnw.cmd -Dtest=AuthenticationIntegrationTests test`.
- Build the executable jar without tests: `.\mvnw.cmd -DskipTests package`.
- Start the application and PostgreSQL: `docker compose up --build`.

## Database

- Compose reads PostgreSQL settings, bootstrap credentials, CORS origin, and JWT settings from the ignored `.env`; keep `APP_JWT_SECRET` Base64-encoded with at least 256 bits.
- Tests use the test-scope H2 driver when those variables are absent. Compose uses PostgreSQL 17 with `postgres:5432` inside the app container; host commands must use `localhost:5432`.
