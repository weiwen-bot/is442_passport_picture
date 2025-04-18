/*
 * ImageResizeResponse.java
 * 
 * This class represents the response DTO for the image resizing operation
 * in the Passport Picture Project.
 * 
 */

package com.passportphoto.dto;

import com.passportphoto.dto.ImageResponse;

/**
 * The {@code ImageResizeResponse} class is a Data Transfer Object (DTO)
 * that encapsulates the result of an image resizing operation, including
 * a status string, a descriptive message, and the base64-encoded image data.
 */

 public class ImageResizeResponse extends ImageResponse {
    public ImageResizeResponse(String status, String message, String image) {
        super(status, message, image);
    }
}
