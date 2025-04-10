/*
 * ImageCropResponse.java
 * 
 * This class represents the response DTO for the image upload operation
 * in the Passport Picture Project.
 * 
 */

package com.passportphoto.dto;
import com.passportphoto.dto.ImageResponse;
/**
 * The {@code ImageCropResponse} class is a Data Transfer Object (DTO)
 * that contains the result of an image upload, including a status,
 * descriptive message, and the uploaded image in base64 format.
 */

 public class ImageCropResponse extends ImageResponse {
    public ImageCropResponse(String status, String message, String image) {
        super(status, message, image);
    }
}
