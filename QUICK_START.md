# Quick Start: Cloudinary → Media Server Migration

## ✅ Migration Status: COMPLETE

**Date**: April 14, 2026  
**Build Status**: ✅ SUCCESS (84.02 MB JAR)  
**Compilation**: ✅ No errors

---

## What Changed?

### For Developers
- No API changes needed - use the same `CloudinaryUploadService` interface
- Same exception handling with `CloudinaryUploadException`
- Same DTO: `CloudinaryUploadResponse`
- **All controllers and services work without modification**

### Infrastructure
- **Old**: Cloudinary Cloud → uploads to Cloudinary CDN
- **New**: Spring App → Media Server (Go) → MinIO (S3-compatible storage)

---

## Running Locally

### Prerequisites
- Docker & Docker Compose
- Go 1.25.0 (optional, if running Media Server from source)
- Java 17+
- PostgreSQL 13+

### Step 1: Start Infrastructure
```powershell
# PostgreSQL + RabbitMQ
docker compose up -d db queue

# Media Server dependencies (MinIO + PostgreSQL + Redis)
cd Media-Server
docker compose up -d
```

### Step 2: Start Media Server (if not using docker compose)
```powershell
cd Media-Server
go run ./cmd/server
# Server runs on http://localhost:3000
```

### Step 3: Start Nexora App
```powershell
cd ..
.\mvnw.cmd spring-boot:run
# App runs on http://localhost:8000
```

### Step 4: Verify Setup
```powershell
# Check Media Server health
curl http://localhost:3000/

# Check app is running
curl http://localhost:8000/swagger-ui.html
```

---

## API Configuration

**application.yaml** now uses:
```yaml
media-server:
  url: http://localhost:3000
  max-file-size: 5242880          # 5 MB
  allowed-image-types: image/jpeg,image/png,image/webp,image/gif
  allowed-media-types: image/jpeg,image/png,image/webp,image/gif,video/mp4,video/mpeg,video/quicktime,video/webm
```

**For production**, update:
```yaml
media-server:
  url: https://media-server.your-domain.com
```

---

## Testing Uploads

### Upload Avatar (Authenticated)
```powershell
# Get JWT token first (via login endpoint)
$token = "Bearer YOUR_JWT_TOKEN_HERE"

# Upload avatar
Invoke-WebRequest `
  -Uri "http://localhost:8000/api/users/1/avatar" `
  -Method Post `
  -Form @{ file = Get-Item "C:\path\to\image.jpg" } `
  -Headers @{ "Authorization" = $token }
```

### Upload Post Media
```powershell
# Create post with media (same authentication required)
$files = Get-ChildItem "C:\path\to\media\*"
$form = @{ 
  title = "My Post"
  content = "Post description"
  files = $files
}

Invoke-WebRequest `
  -Uri "http://localhost:8000/api/posts" `
  -Method Post `
  -Form $form `
  -Headers @{ "Authorization" = $token }
```

---

## File Storage

### URLs Format
- **Avatar**: `http://localhost:9000/media/filename.jpg`
- **Post Media**: `http://localhost:9000/media/filename.mp4`

### Location
- **MinIO Console**: http://localhost:9001
  - Username: `minioadmin`
  - Password: `minioadmin`
- **Bucket**: `media` (configurable via env vars)

---

## Key Differences from Cloudinary

| Feature | Cloudinary | Media Server |
|---------|-----------|--------------|
| Upload Endpoint | SDK method | POST `/api/v1/upload/` |
| Storage | CDN | MinIO (S3-compatible) |
| Deletion | `destroy(publicId)` | File orphaned (no cleanup) |
| Transformations | Built-in | Not included |
| Cost | Pay-as-you-go | Self-hosted |

---

## Troubleshooting

### Media Server connection refused
```
Error: Connection to localhost:3000 refused
```
**Solution**: Ensure Media Server is running: `go run ./cmd/server` in Media-Server folder

### File upload returns 400
**Check**:
- File size < 5 MB
- MIME type is in allowed list
- Form field name is `file` (not `media` or other)

### MinIO bucket not found
```powershell
# MinIO auto-creates bucket on first upload
# Or manually at: http://localhost:9001
```

### PostgreSQL connection error
```powershell
# Ensure db is running
docker compose ps db
# If not, start it:
docker compose up -d db
```

---

## Environment Variables (Optional)

For Media Server (in `Media-Server/.env`):
```
SERVER_ADDR=:3000
MINIO_BUCKET=media
MINIO_ENDPOINT=localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
DATABASE_URL=postgres://postgres:postgres@localhost:5432/media-db
```

---

## Rollback Plan

If needed to revert to Cloudinary:
1. Revert `pom.xml` - restore Cloudinary dependencies
2. Revert `application.yaml` - restore Cloudinary config
3. Revert `CloudinaryConfig.java` - restore Cloudinary bean
4. Revert `CloudinaryUploadServiceImpl.java` - restore SDK implementation
5. Rebuild: `.\mvnw.cmd clean package`

---

## Support

**Migration Documentation**: `MIGRATION_COMPLETE.md`  
**Issues**: Check logs in console or `target/logs/`  
**Questions**: Refer to Media Server README in `Media-Server/README.md`

---

✅ **Ready to test!**

