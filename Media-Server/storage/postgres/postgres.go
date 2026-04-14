package postgres

import (
	"context"

	"Media-Server/internal/configEnv"
	"Media-Server/utils"

	"github.com/jackc/pgx/v5/pgxpool"
)

var Pool *pgxpool.Pool

func Init() error {
	var err error
	Pool, err = pgxpool.New(context.Background(), configEnv.Cfg.DatabaseURL)
	if err != nil {
		return err
	}

	if err = Pool.Ping(context.Background()); err != nil {
		return err
	}

	utils.Log.Info("PostgreSQL connected")
	return nil
}

func Close() {
	if Pool != nil {
		Pool.Close()
	}
}
