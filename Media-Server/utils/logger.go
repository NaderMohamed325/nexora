package utils

import (
	"mime/multipart"
	"os"

	"go.uber.org/zap"
)

var Log *zap.Logger

func LoggerInit() {
	var err error
	Log, err = zap.NewProduction()
	if err != nil {
		panic(err)
	}
}

func SaveFile(file multipart.File, filename string) (*os.File, error) {
	out, err := os.Create(filename)
	if err != nil {
		Log.Error("Failed to create file", zap.String("filename", filename), zap.Error(err))
		return nil, err
	}
	_, err = out.ReadFrom(file)
	if err != nil {
		Log.Error("Failed to save file", zap.String("filename", filename), zap.Error(err))
		return nil, err
	}
	return out, nil
}
