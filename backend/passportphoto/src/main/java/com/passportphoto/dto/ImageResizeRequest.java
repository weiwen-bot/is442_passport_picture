package com.passportphoto.dto;

import org.springframework.web.multipart.MultipartFile;

public class ImageResizeRequest {
    private MultipartFile image;
    private String country;

    public ImageResizeRequest() {}

    public ImageResizeRequest(MultipartFile image, String country) {
        this.image = image;
        this.country = country;
    }

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
