package main

import (
	"context"
	"errors"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"Media-Server/internal/configEnv"
	"Media-Server/internal/handler"
	"Media-Server/storage/minio"
	"Media-Server/storage/postgres"
	"Media-Server/storage/redis"
	"Media-Server/utils"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"go.uber.org/zap"
)

func main() {
	if err := configEnv.Load(); err != nil {
		panic(err)
	}

	utils.LoggerInit()
	defer utils.Log.Sync()

	utils.Log.Info("Initializing database connection")
	if err := postgres.Init(); err != nil {
		utils.Log.Fatal("Failed to connect to database", zap.Error(err))
	}
	defer postgres.Close()

	utils.Log.Info("Initializing Redis connection")
	if err := redis.Init(); err != nil {
		utils.Log.Warn("Failed to connect to Redis, continuing without it", zap.Error(err))
	}
	defer redis.Close()

	utils.Log.Info("Initializing MinIO client")
	if err := minio.Init(); err != nil {
		utils.Log.Fatal("Failed to connect to MinIO", zap.Error(err))
	}

	r := chi.NewRouter()
	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)

	r.Get("/", func(w http.ResponseWriter, r *http.Request) {
		w.Write([]byte("Media Server v1.0"))
	})

	r.Mount("/api/v1/upload", handler.UploadRouter())

	srv := &http.Server{
		Addr:         configEnv.Cfg.ServerAddr,
		Handler:      r,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	go func() {
		utils.Log.Info("Starting server", zap.String("addr", configEnv.Cfg.ServerAddr))
		if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			utils.Log.Fatal("Server failed", zap.Error(err))
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	utils.Log.Info("Shutting down server...")
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		utils.Log.Fatal("Server forced to shutdown", zap.Error(err))
	}

	utils.Log.Info("Server exited")
}
