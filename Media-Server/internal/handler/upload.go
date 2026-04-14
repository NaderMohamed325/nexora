package handler

import (
	"Media-Server/internal/configEnv"
	storageMinio "Media-Server/storage/minio"
	"Media-Server/utils"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"strings"

	"github.com/go-chi/chi/v5"
	minioSDK "github.com/minio/minio-go/v7"

	"go.uber.org/zap"
)

func UploadRouter() chi.Router {
	r := chi.NewRouter()
	r.Post("/", uploadHandler)
	r.Post("/path", uploadFromPathHandler)
	r.Post("/stream", streamHandler)
	// Keep backward compatibility for existing clients using the typo path.
	r.Post("/tus", uploadVideoTus)
	return r
}

func streamHandler(w http.ResponseWriter, r *http.Request) {
	playlistPath, err := streamFromMinIO(r)
	if err != nil {
		utils.Log.Error("Failed to stream from MinIO", zap.Error(err))
		http.Error(w, "Failed to stream from MinIO", http.StatusInternalServerError)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(map[string]string{
		"message":      "Stream generated successfully",
		"playlistPath": playlistPath,
	})
}

/*
@Summary: uploadHandler
@Description: Handles file uploads and stores them in MinIO.
@Tags: Upload
@Accept: multipart/form-data
@Param: file formData file true "File to upload"
@Success: 200 {object} map[string]string "fileUrl"
@Failure: 400 {object} map[string]string "error"
@Router: /upload [post]
*/
func uploadHandler(w http.ResponseWriter, r *http.Request) {
	err := r.ParseMultipartForm(32 << 20)
	if err != nil {
		utils.Log.Error("Failed to parse multipart form", zap.Error(err))
		return
	}
	file, header, err := r.FormFile("file")
	if err != nil {
		return
	}
	defer file.Close()

	utils.Log.Info("Received file upload", zap.String("filename", header.Filename))
	minoClient := storageMinio.GetClient()
	minoBucketName := configEnv.Cfg.MinIOBucket
	ObjectOptions := storageMinio.PutObjectOptions(header.Header.Get("Content-Type"), "max-age=604800", map[string]string{
		"original-filename": header.Filename,
	})

	_, err = minoClient.PutObject(r.Context(), minoBucketName, header.Filename, file, header.Size, ObjectOptions)
	if err != nil {
		utils.Log.Error("Failed to upload file to MinIO", zap.Error(err))
		return
	}
	fileUrl := minoClient.EndpointURL().String() + "/" + minoBucketName + "/" + header.Filename

	w.WriteHeader(http.StatusOK)
	err = json.NewEncoder(w).Encode(map[string]string{
		"fileUrl": fileUrl,
	})
	if err != nil {
		return
	}

}

type UploadPathRequest struct {
	FilePath string `json:"filepath"`
}

func uploadFromPathHandler(w http.ResponseWriter, r *http.Request) {
	var req UploadPathRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	file, err := os.Open(req.FilePath)
	if err != nil {
		utils.Log.Error("Failed to open file", zap.String("filePath", req.FilePath), zap.Error(err))
		http.Error(w, "Failed to open file", http.StatusInternalServerError)
		return
	}
	defer file.Close()

	stat, err := file.Stat()
	if err != nil {
		utils.Log.Error("Failed to get file stats", zap.String("filePath", req.FilePath), zap.Error(err))
		http.Error(w, "Failed to get file stats", http.StatusInternalServerError)
		return
	}

	utils.Log.Info("Received file upload from path", zap.String("filename", stat.Name()))
	minoClient := storageMinio.GetClient()
	minoBucketName := configEnv.Cfg.MinIOBucket
	ObjectOptions := storageMinio.PutObjectOptions("video/mp4", "max-age=604800", map[string]string{
		"original-filename": stat.Name(),
	})

	_, err = minoClient.PutObject(r.Context(), minoBucketName, stat.Name(), file, stat.Size(), ObjectOptions)
	if err != nil {
		utils.Log.Error("Failed to upload file to MinIO", zap.Error(err))
		http.Error(w, "Failed to upload file to MinIO", http.StatusInternalServerError)
		return
	}
	fileUrl := minoClient.EndpointURL().String() + "/" + minoBucketName + "/" + stat.Name()

	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]string{
		"fileUrl": fileUrl,
	})
}

type FileUrlRequest struct {
	FileUrl string `json:"fileUrl"`
}

func streamFromMinIO(r *http.Request) (string, error) {
	fileUrlRequestBody := FileUrlRequest{}
	if err := json.NewDecoder(r.Body).Decode(&fileUrlRequestBody); err != nil {
		utils.Log.Error("Failed to decode request body", zap.Error(err))
		return "", err
	}

	objectKey := fileUrlRequestBody.FileUrl
	if strings.Contains(objectKey, "/") {
		parts := strings.Split(objectKey, "/")
		objectKey = parts[len(parts)-1]
	}

	client := storageMinio.GetClient()
	object, err := client.GetObject(context.Background(), configEnv.Cfg.MinIOBucket, objectKey, minioSDK.GetObjectOptions{})
	if err != nil {
		utils.Log.Error("Failed to get object from MinIO", zap.String("fileUrl", objectKey), zap.Error(err))
		return "", err
	}
	defer object.Close()

	// fix 1: use TempFile so dir always exists
	tmpFile, err := os.CreateTemp("", "video-*.mp4")
	if err != nil {
		return "", fmt.Errorf("create temp file: %w", err)
	}
	tmpPath := tmpFile.Name()
	defer os.Remove(tmpPath)

	if _, err = io.Copy(tmpFile, object); err != nil {
		tmpFile.Close()
		return "", fmt.Errorf("download from minio: %w", err)
	}
	tmpFile.Close()

	// fix 2: dedicated output dir per video
	cacheKey := strings.ReplaceAll(objectKey, "/", "_")
	outDir := filepath.Join("/tmp/hls", cacheKey)
	if err := os.MkdirAll(outDir, 0755); err != nil {
		return "", fmt.Errorf("mkdir: %w", err)
	}

	// fix 3: capture ffmpeg error
	if err := videoToHLS(tmpPath, outDir); err != nil {
		return "", fmt.Errorf("ffmpeg: %w", err)
	}

	return filepath.Join(outDir, "master.m3u8"), nil
}

func videoToHLS(inputPath, outDir string) error {
	cmd := exec.Command("ffmpeg",
		"-i", inputPath,
		"-filter_complex", "[0:v]split=2[v1][v2];[v1]scale=w=1280:h=720[v1out];[v2]scale=w=854:h=480[v2out]",
		"-map", "[v1out]", "-c:v:0", "libx264", "-b:v:0", "2800k",
		"-map", "[v2out]", "-c:v:1", "libx264", "-b:v:1", "1400k",
		"-map", "a:0", "-c:a", "aac", "-b:a:0", "128k",
		"-f", "hls",
		"-hls_time", "10",
		"-hls_playlist_type", "vod",
		"-master_pl_name", "master.m3u8",
		"-var_stream_map", "v:0,a:0 v:1,a:0",
		filepath.Join(outDir, "stream_%v.m3u8"), // fix 4: output to outDir
	)

	cmd.Stderr = os.Stderr // see ffmpeg logs
	return cmd.Run()
}
