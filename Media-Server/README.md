# Media Server

A small Go API for uploading files to MinIO.

## What it does

- Starts an HTTP server (default `:3000`)
- Connects to PostgreSQL (required)
- Connects to Redis (optional at startup; server continues if Redis is unavailable)
- Connects to MinIO and creates the configured bucket if it does not already exist
- Exposes:
  - `GET /` health-style text response
  - `POST /api/v1/upload/` multipart upload endpoint

## Project structure

- `cmd/server/main.go` - app entrypoint and startup wiring
- `internal/config/config.go` - environment config loader
- `internal/handler/upload.go` - upload HTTP handler
- `storage/minio/minio.go` - MinIO client init and bucket ensure
- `storage/postgres/postgres.go` - PostgreSQL pool init
- `storage/redis/redis.go` - Redis client init

## Requirements

- Go `1.25.0` (from `go.mod`)
- Docker + Docker Compose (for local dependencies)

## Environment variables

The app loads `.env` automatically (via `godotenv`).

| Variable | Default | Used for |
|---|---|---|
| `SERVER_ADDR` | `:3000` | HTTP listen address |
| `DATABASE_URL` | `postgres://postgres:postgres@localhost:5432/media-db` | PostgreSQL connection |
| `REDIS_ADDR` | `localhost:6379` | Redis connection |
| `MINIO_ENDPOINT` | `localhost:9000` | MinIO API endpoint |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO access key |
| `MINIO_SECRET_KEY` | `minioadmin` | MinIO secret key |
| `MINIO_BUCKET` | `media-bucket` | Bucket name for uploads |
| `MINIO_USE_SSL` | `false` | Use HTTPS for MinIO |
| `UPLOAD_DIR` | `./uploads` | Reserved; currently not used by upload handler |
| `MAX_FILE_SIZE` | `10485760` | Reserved; currently not enforced in handler |

> Note: the repo `.env` currently sets `MINIO_BUCKET=media`. If Docker init creates `media-bucket`, the app will create `media` on startup unless you align these values.

## Run locally

1. Start dependencies:

```powershell
docker compose up -d
```

2. Run the server:

```powershell
go run ./cmd/server
```

3. Check base endpoint:

```powershell
curl http://localhost:3000/
```

Expected response body:

```text
Media Server v1.0
```

## Upload API

### `POST /api/v1/upload/`

- Content type: `multipart/form-data`
- Required form field: `file`

Example:

```powershell
curl -X POST "http://localhost:3000/api/v1/upload/" `
  -F "file=@C:\path\to\your\image.png"
```

Example response:

```json
{
  "fileUrl": "http://localhost:9000/media/your-file-name.png"
}
```

`fileUrl` bucket segment depends on `MINIO_BUCKET`.

## MinIO console

- URL: `http://localhost:9001`
- Username: `minioadmin`
- Password: `minioadmin`

## Dev checks

Run all package tests/build checks:

```powershell
go test ./...
```

Current repo state: packages compile and there are no test files yet.

## Current limitations

- No authentication/authorization on upload endpoint
- Upload handler has minimal error responses (some failures return without response body)
- Max file size from config is not yet enforced in handler
- Multiple-file upload and progress tracking are TODOs (`internal/handler/upload.go`)

