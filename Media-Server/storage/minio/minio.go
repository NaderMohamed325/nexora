package minio

import (
	"context"

	"Media-Server/internal/configEnv"
	"Media-Server/utils"
	"github.com/minio/minio-go/v7"
	minioCreds "github.com/minio/minio-go/v7/pkg/credentials"
	"go.uber.org/zap"
)

var Client *minio.Client

func Init() error {
	var err error
	Client, err = minio.New(configEnv.Cfg.MinIOEndpoint, &minio.Options{
		Creds:  minioCreds.NewStaticV4(configEnv.Cfg.MinIOKey, configEnv.Cfg.MinIOSecret, ""),
		Secure: configEnv.Cfg.MinIOUseSSL,
	})

	if err != nil {
		return err
	}

	ctx := context.Background()
	exists, err := Client.BucketExists(ctx, configEnv.Cfg.MinIOBucket)
	if err != nil {
		return err
	}
	if !exists {
		if err = Client.MakeBucket(ctx, configEnv.Cfg.MinIOBucket, minio.MakeBucketOptions{}); err != nil {
			return err
		}
	}

	// Set public read policy
	policy := `{
        "Version": "2012-10-17",
        "Statement": [{
            "Effect": "Allow",
            "Principal": {"AWS": ["*"]},
            "Action": ["s3:GetObject"],
            "Resource": ["arn:aws:s3:::` + configEnv.Cfg.MinIOBucket + `/*"]
        }]
    }`

	if err = Client.SetBucketPolicy(ctx, configEnv.Cfg.MinIOBucket, policy); err != nil {
		return err
	}

	utils.Log.Info("MinIO connected", zap.String("bucket", configEnv.Cfg.MinIOBucket))

	return nil
}

func GetClient() *minio.Client {
	return Client
}

func PutObjectOptions(ContentType string, CacheControl string, UserMetadata map[string]string) minio.PutObjectOptions {
	return minio.PutObjectOptions{
		ContentType:  ContentType,
		CacheControl: CacheControl,
		UserMetadata: UserMetadata,
	}
}
