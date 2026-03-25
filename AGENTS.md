# AGENTS.md

## Project Snapshot

- Stack: Spring Boot 4.0.3, Java 17+, Spring Security (JWT), JPA/PostgreSQL, RabbitMQ, Cloudinary, Bucket4j, springdoc
  OpenAPI.
- Main entrypoint: `src/main/java/com/neo/nexora/NexoraApplication.java` loads `.env` into JVM system properties before
  Spring starts.
- API shape is controller -> service -> repository, with DTO boundaries and `ApiResponse<T>` wrappers (
  `src/main/java/com/neo/nexora/dto/ApiResponse.java`).

## Architecture and Data Flow (Read This First)

- Auth flow: `AuthController` -> `AuthServiceImpl` -> `JwtUtil` and `UserRepository`; logout blacklists JWTs via
  `TokenBlacklistService` + `BlacklistedTokenRepository`.
- Security is stateless JWT: `SecurityConfig` installs `JwtAuthenticationFilter` before
  `UsernamePasswordAuthenticationFilter`; most `/api/**` endpoints require auth.
- Account deletion is asynchronous: `UserServiceImpl.deactivateAccount()` marks `PENDING_DELETION` + timestamp, then
  `UserDeletionProducerImpl` (2 AM cron) publishes RabbitMQ batches, `UserDeletionConsumerImpl` deletes in sub-batches
  of 500.
- Username existence checks use a Bloom filter fast-path (`UserLookUpServiceImpl`, `BloomFilterConfig`) before hitting
  DB in `AuthServiceImpl`.
- Media flow: `PostServiceImpl` and `UserController` call `CloudinaryUploadServiceImpl`; Cloudinary URLs are stored on
  entities (post media list, user avatar URL).
- Text search is DB-optimized with PostgreSQL trigram extension/indexes created at startup in `DatabaseInitializer`.

## Local Dev Workflow

- Bring dependencies up first (Postgres + RabbitMQ):
  ```powershell
  docker compose up -d db queue
  ```
- Start app:
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
- Test command (verified in this repo):
  ```powershell
  .\mvnw.cmd -q test
  ```
- Docs/inspection endpoints: Swagger UI at `/swagger-ui/index.html` (or `/swagger-ui.html`), OpenAPI JSON at
  `/v3/api-docs`.

## Project-Specific Conventions

- Return `ApiResponse.success(...)` / `ApiResponse.error(...)` from controllers; keep message strings explicit and
  user-facing.
- Controllers often catch `IllegalArgumentException`/`RuntimeException` directly (example: `AuthController`) in addition
  to global validation handling.
- Authorization is split between route config (`SecurityConfig`) and method-level `@PreAuthorize` checks in controllers.
- Ownership checks are done in services (e.g., `PostServiceImpl.updatePost/deletePostById`,
  `CommentServiceImpl.updateComment/deleteComment`).
- Pagination defaults are usually `page=0`, `size=10` (or `20` for slugs); follow existing signatures when adding list
  endpoints.

## Integration Notes and Gotchas

- `spring.jpa.hibernate.ddl-auto=create-drop` in `src/main/resources/application.yaml` resets schema on restart; do not
  assume persistent local data.
- `RateLimitingFilter` uses one shared Bucket4j bucket bean for all traffic (`RateLimitConfig` +
  `filter/RateLimitingFilter.java`), i.e., global not per-user/IP.
- RabbitMQ queue/exchange/routing constants live in `RabbitMQConfig`; reuse these constants in any new producer/consumer
  work.
- Slug click count is incremented with a bulk update (`SlugRepository.incrementClickCount`) + re-read pattern in
  `SlugServiceImpl` to avoid stale entity state.
- `FollowController` is currently a stub (no endpoints); follow features are service-only right now.

## Key Directories

- `src/main/java/com/neo/nexora/controller`: HTTP contracts and response wrapping conventions.
- `src/main/java/com/neo/nexora/service`: business rules, auth/account lifecycle, queue producers/consumers.
- `src/main/java/com/neo/nexora/config`: security, scheduler, messaging, Cloudinary, OpenAPI, DB bootstrap.
- `src/main/java/com/neo/nexora/repository`: Spring Data interfaces + custom JPQL/native queries.
- `src/main/resources/application.yaml`: runtime defaults for DB, JWT, RabbitMQ, Cloudinary, port.

