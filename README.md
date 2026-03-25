# Nexora

Nexora is a Spring Boot backend for a social platform with JWT auth, posts/comments/likes, user profiles, slug resolution, async account deletion, and RabbitMQ notifications.

## Tech Stack

- Java 17+
- Spring Boot 4.0.3 (Web MVC, Security, Validation, Data JPA, AMQP)
- PostgreSQL
- RabbitMQ
- Redis (idempotency/cache plumbing)
- Cloudinary (media uploads)
- Bucket4j (global rate limiting)
- springdoc OpenAPI

## Project Structure

- `src/main/java/com/neo/nexora/controller`: REST endpoints and HTTP contracts
- `src/main/java/com/neo/nexora/service`: business logic and integrations
- `src/main/java/com/neo/nexora/repository`: JPA repositories and custom DB queries
- `src/main/java/com/neo/nexora/config`: security, messaging, scheduler, cloud, db bootstrapping
- `src/main/java/com/neo/nexora/filter`: request filters (JWT auth, rate limiting)
- `src/main/resources/application.yaml`: runtime configuration
- `postman/Nexora-All-APIs.postman_collection.json`: importable API collection

## Architecture and Data Flow

### Request pipeline

1. Request enters Spring Security chain (`SecurityConfig`).
2. `JwtAuthenticationFilter` resolves bearer token and populates auth context.
3. `RateLimitingFilter` enforces a shared Bucket4j bucket.
4. Controller validates DTO and calls a service.
5. Service enforces business/ownership rules and calls repository or integration services.
6. Controller returns `ApiResponse<T>`.

### Authentication and authorization

- Auth endpoints live under `/api/auth`.
- Most `/api/**` routes require authentication.
- Method-level access control is enforced with `@PreAuthorize`.
- Logout is implemented by token blacklisting.

### Async deletion flow

- User deactivation marks account for deletion.
- Scheduler (`UserDeletionProducer`) publishes user ID batches to RabbitMQ.
- Consumer (`UserDeletionConsumer`) deletes users in queue-driven batches.

### Media flow

- Post media and avatars are uploaded through `CloudinaryUploadService`.
- URLs are persisted in entities and returned in DTOs.

### Search optimization

`DatabaseInitializer` creates `pg_trgm` extension and trigram indexes for faster username/title/content search.

## Local Development

### 1) Start dependencies

```powershell
docker compose up -d db queue cahce
```

> Note: service key is `cahce` in `docker-compose.yml`.

### 2) Run backend

```powershell
.\mvnw.cmd spring-boot:run
```

### 3) Run tests

```powershell
.\mvnw.cmd -q test
```

### 4) API docs

- Swagger UI: `http://localhost:8000/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8000/v3/api-docs`

## API Conventions

- Controllers return `ApiResponse.success(...)` and `ApiResponse.error(...)`.
- Pagination defaults are usually `page=0`, `size=10` (`size=20` for slugs listing).
- Protected requests need `Authorization: Bearer <jwt>`.
- Multipart endpoints (`/api/posts`, `/api/posts/{id}`, `/api/users/{id}/avatar`) require `form-data` payloads.

## Important Runtime Notes

- `spring.jpa.hibernate.ddl-auto=create-drop`: schema resets on app restart.
- CORS allows `http://localhost:5173` by default.
- Rate limiting is global (single shared bucket), not per-user/IP.
- RabbitMQ constants live in `RabbitMQConfig`; reuse these constants in producer/consumer code.

## Function-Level Documentation

See `docs/FUNCTION_REFERENCE.md` for endpoint-by-endpoint and service-method reference.

