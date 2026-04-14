package redis

import (
	"context"

	"Media-Server/internal/configEnv"
	"Media-Server/utils"

	"github.com/redis/go-redis/v9"
)

var Client *redis.Client

func Init() error {
	Client = redis.NewClient(&redis.Options{
		Addr: configEnv.Cfg.RedisAddr,
	})

	if err := Client.Ping(context.Background()).Err(); err != nil {
		return err
	}

	utils.Log.Info("Redis connected")
	return nil
}

func Close() {
	if Client != nil {
		err := Client.Close()
		if err != nil {
			return
		}
	}
}
