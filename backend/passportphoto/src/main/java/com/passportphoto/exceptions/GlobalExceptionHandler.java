package com.passportphoto.exceptions;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.security.auth.login.FailedLoginException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.passportphoto.exceptions.FailedProcessingException;
import com.passportphoto.exceptions.ImageInvalidFormatException;
import com.passportphoto.exceptions.ImageTooLargeException;
import ai.onnxruntime.*;


/**
 * Global exception handler for the application.
 * Provides centralized exception handling for image-related errors and AI processing failures.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles invalid image format exceptions.
     * Returns HTTP 415 (Unsupported Media Type).
     */

    @ExceptionHandler(ImageInvalidFormatException.class)
    public ResponseEntity<Object> handleImageInvalidFormatException(ImageInvalidFormatException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }


    /**
     * Handles cases where an image exceeds the allowed file size.
     * Returns HTTP 413 (Payload Too Large).
     */
    @ExceptionHandler(ImageTooLargeException.class)
    public ResponseEntity<Object> handleImageTooLargeException(ImageTooLargeException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    /**
     * Handles AI model processing failures.
     * Returns HTTP 500 (Internal Server Error) with an error message.
     */
    @ExceptionHandler(OrtException.class)
    public ResponseEntity<Map<String, String>> handleOrtException(OrtException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("error", "AI model processing failed: " + e.getMessage()));
    }

    /**
     * Handles general exceptions that are not specifically caught.
     * Returns HTTP 500 (Internal Server Error) with a generic error message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception e) {
        // Return a custom error message with HTTP 500 status
        return new ResponseEntity<>("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleIOException(Exception e) {
        // Return a custom error message with HTTP 500 status
        return new ResponseEntity<>("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }



}
