package configEnv

import (
	"os"

	"github.com/joho/godotenv"
)

type Config struct {
	ServerAddr    string
	DatabaseURL   string
	RedisAddr     string
	MinIOEndpoint string
	MinIOKey      string
	MinIOSecret   string
	MinIOBucket   string
	MinIOUseSSL   bool
	UploadDir     string
	MaxFileSize   int64
	TusUploadUrl  string
}

var Cfg *Config

func Load() error {
	_ = godotenv.Load()

	Cfg = &Config{
		ServerAddr:    getEnv("SERVER_ADDR", ":3000"),
		DatabaseURL:   getEnv("DATABASE_URL", "postgres://postgres:postgres@localhost:5432/media-db"),
		RedisAddr:     getEnv("REDIS_ADDR", "localhost:6379"),
		MinIOEndpoint: getEnv("MINIO_ENDPOINT", "localhost:9000"),
		MinIOKey:      getEnv("MINIO_ACCESS_KEY", "minioadmin"),
		MinIOSecret:   getEnv("MINIO_SECRET_KEY", "minioadmin"),
		MinIOBucket:   getEnv("MINIO_BUCKET", "media-bucket"),
		MinIOUseSSL:   getEnv("MINIO_USE_SSL", "false") == "true",
		UploadDir:     getEnv("UPLOAD_DIR", "./uploads"),
		MaxFileSize:   parseInt64(getEnv("MAX_FILE_SIZE", "10485760"), 10*1024*1024),
		TusUploadUrl:  getEnv("TUS_UPLOAD_URL", "http://localhost:1080/files/"),
	}

	return nil
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

func parseInt64(value string, defaultValue int64) int64 {
	var n int64
	for _, c := range value {
		if c < '0' || c > '9' {
			return defaultValue
		}
		n = n*10 + int64(c-'0')
	}
	return n
}
