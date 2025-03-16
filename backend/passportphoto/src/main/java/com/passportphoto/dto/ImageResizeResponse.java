package com.passportphoto.dto;

public class ImageResizeResponse {
    private String status;
    private String message;
    private String image;

    public ImageResizeResponse() {}

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
