package com.passportphoto.dto;

public class ImageResizeResponse {
    private final String status;
    private final String message;
    private final String image;

    public ImageResizeResponse(String status, String message, String image) {
        this.status = status;
        this.message = message;
        this.image = image;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getImage() {
        return image;
    }
}
