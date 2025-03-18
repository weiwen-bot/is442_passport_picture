package com.passportphoto.exceptions;

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

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ImageInvalidFormatException.class)
    public ResponseEntity<Object> handleImageInvalidFormatException(ImageInvalidFormatException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(ImageTooLargeException.class)
    public ResponseEntity<Object> handleImageTooLargeException(ImageTooLargeException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    // @ExceptionHandler(ImageInvalidFormatException.class)
    // public ResponseEntity<Object> handleFailedProcessingException(FailedProcessingException e) {
    //     return new ResponseEntity<>(e.getMessage(), HttpStatus.PAYLOAD_TOO_LARGE);
    // }
    @ExceptionHandler(OrtException.class)
    public ResponseEntity<Map<String, String>> handleOrtException(OrtException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("error", "AI model processing failed: " + e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception e) {
        // Return a custom error message with HTTP 500 status
        return new ResponseEntity<>("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }



}
