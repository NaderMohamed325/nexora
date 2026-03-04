package com.neo.nexora.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a Cloudinary upload or delete operation fails.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class CloudinaryUploadException extends RuntimeException {

    public CloudinaryUploadException(String message) {
        super(message);
    }

    public CloudinaryUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}

