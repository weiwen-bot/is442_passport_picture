/*
 * ImageUploadResponse.java
 * 
 * This class represents the response DTO for the image upload operation
 * in the Passport Picture Project.
 * 
 */

package com.passportphoto.dto;

/**
 * The {@code ImageUploadResponse} class is a Data Transfer Object (DTO)
 * that contains the result of an image upload, including a status,
 * descriptive message, and the uploaded image in base64 format.
 */

public class ImageUploadResponse {

    /** Status of the upload operation (e.g., "success", "error") */
    private String status;

    /** Message providing additional details about the upload result */
    private String message;

    /** Base64-encoded string of the uploaded image */
    private String image;

    /**
     * Constructs an {@code ImageUploadResponse} with the given status, message, and image.
     *
     * @param status  the status of the upload operation
     * @param message a descriptive message
     * @param image   the base64-encoded uploaded image
     */
    public ImageUploadResponse(String status, String message, String image) {
        this.status = status;
        this.message = message;
        this.image = image;
    }

    /**
     * Gets the status of the upload operation.
     *
     * @return the status string
     */
    public String getStatus() { 
        return status; 
    }

    /**
     * Gets the message associated with the upload operation.
     *
     * @return the descriptive message
     */
    public String getMessage() { 
        return message; 
    }

    /**
     * Gets the base64-encoded uploaded image.
     *
     * @return the image string
     */
    public String getImage() { 
        return image; 
    }
}
