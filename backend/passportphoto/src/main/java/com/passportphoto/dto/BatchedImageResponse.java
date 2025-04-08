package com.passportphoto.dto;

import java.util.List;

public class BatchedImageResponse {
    private List<String> processedImage;

    public BatchedImageResponse(List<String> processedImage) {
        this.processedImage = processedImage;
    }

    public List<String> getProcessedImage() {
        return processedImage;
    }
}
