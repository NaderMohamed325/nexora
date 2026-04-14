package handler

import (
	"Media-Server/internal/configEnv"
	"Media-Server/utils"
	"encoding/json"
	"io"
	"net/http"
	"net/url"
	"os"
	"sync"

	"go.uber.org/zap"
)
import "github.com/bdragon300/tusgo"

type TusUploadRequest struct {
	FilePath string `json:"filePath"`
}

var cl *tusgo.Client
var uploadLocation tusgo.Upload
var tusInitOnce sync.Once

func initTusClient() {
	tusInitOnce.Do(func() {
		baseURL, err := url.Parse(configEnv.Cfg.TusUploadUrl)
		if err != nil {
			utils.Log.Fatal("Invalid TUS URL", zap.Error(err))
		}
		uploadLocation = tusgo.Upload{Location: configEnv.Cfg.TusUploadUrl, RemoteSize: 1024 * 1024}
		cl = tusgo.NewClient(http.DefaultClient, baseURL)
		utils.Log.Info("TUS upload initialized", zap.String("endpoint", configEnv.Cfg.TusUploadUrl))
	})
}

func getFilePath(r *http.Request) string {
	reqBody := TusUploadRequest{}
	if err := json.NewDecoder(r.Body).Decode(&reqBody); err != nil {
		return ""
	}
	return reqBody.FilePath
}

func uploadVideoTus(w http.ResponseWriter, r *http.Request) {
	initTusClient()
	filePath := getFilePath(r)
	if filePath == "" {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}
	utils.Log.Info("Received TUS upload request", zap.String("filePath", filePath))
	f, err := os.Open(filePath)
	if err != nil {
		utils.Log.Error("Failed to open file", zap.String("filePath", filePath), zap.Error(err))
		http.Error(w, "Failed to open file", http.StatusInternalServerError)
		return
	}
	defer f.Close()

	s := tusgo.NewUploadStream(cl, &uploadLocation)
	// Set stream and file pointers to be equal to the remote pointer
	if _, err = s.Sync(); err != nil {
		utils.Log.Error("Failed to sync upload stream", zap.String("filePath", filePath), zap.Error(err))
		http.Error(w, "Failed to sync upload stream", http.StatusInternalServerError)
		return
	}
	if _, err = f.Seek(s.Tell(), io.SeekStart); err != nil {
		utils.Log.Error("Failed to seek file", zap.String("filePath", filePath), zap.Error(err))
		http.Error(w, "Failed to seek file", http.StatusInternalServerError)
		return
	}
	_, err = io.Copy(s, f)
	if err != nil {
		utils.Log.Error("Failed to copy file to upload stream", zap.String("filePath", filePath), zap.Error(err))
		http.Error(w, "Failed to copy file to upload stream", http.StatusInternalServerError)
		return
	}

	uploadURL := uploadLocation.Location
	if s.LastResponse != nil {
		if location := s.LastResponse.Header.Get("Location"); location != "" {
			uploadURL = location
		}
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]string{
		"uploadUrl": uploadURL,
	})
}
