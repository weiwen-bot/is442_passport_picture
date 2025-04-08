package com.passportphoto.dto;
public abstract class ImageResponse {
    /** Status of the resize operation (e.g., "success", "failure") */
    protected final String status;
    /** Message providing additional information about the operation */
    protected final String message;
    /** Base64-encoded string of the resized image */
    protected final String image;

    public ImageResponse(String status, String message, String image){
        this.status = status;
        this.message = message;
        this.image = image;
    }
    /**
     * Gets the status of the image operation.
     *
     * @return the status string
     */
    public String getStatus() {
        return status;
    }
    /**
     * Gets the message associated with the image operation.
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
