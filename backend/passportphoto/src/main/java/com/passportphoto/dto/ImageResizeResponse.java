/*
 * ImageResizeResponse.java
 * 
 * This class represents the response DTO for the image resizing operation
 * in the Passport Picture Project.
 * 
 */

package com.passportphoto.dto;

/**
 * The {@code ImageResizeResponse} class is a Data Transfer Object (DTO)
 * that encapsulates the result of an image resizing operation, including
 * a status string, a descriptive message, and the base64-encoded image data.
 */

 public class ImageResizeResponse {
    
    /** Status of the resize operation (e.g., "success", "failure") */
    private final String status;

    /** Message providing additional information about the operation */
    private final String message;

    /** Base64-encoded string of the resized image */
    private final String image;

    /**
     * Constructs an {@code ImageResizeResponse} with the given status, message, and image.
     *
     * @param status  the status of the operation
     * @param message a message describing the result
     * @param image   the base64-encoded image string
     */
    public ImageResizeResponse(String status, String message, String image) {
        this.status = status;
        this.message = message;
        this.image = image;
    }

    /**
     * Gets the status of the image resize operation.
     *
     * @return the status string
     */
    public String getStatus() {
        return status;
    }

    /**
     * Gets the message associated with the image resize operation.
     *
     * @return the descriptive message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the base64-encoded image string.
     *
     * @return the image string
     */
    public String getImage() {
        return image;
    }
}
