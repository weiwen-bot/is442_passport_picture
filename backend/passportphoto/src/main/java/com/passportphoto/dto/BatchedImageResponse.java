/*
 * BatchedImageResponse.java
 *
 * This class represents the response DTO for batched automated image processing
 * in the Passport Picture Project.
 *
 */

package com.passportphoto.dto;

import java.util.List;

/**
 * The {@code BatchedImageResponse} class is a Data Transfer Object (DTO)
 * that holds a list of base64-encoded processed images resulting from
 * batch image generation.
 */
public class BatchedImageResponse {

    /** A list of base64-encoded strings representing processed images. */
    private List<String> processedImage;

    /**
     * Constructs a {@code BatchedImageResponse} with a list of processed images.
     *
     * @param processedImage the list of base64-encoded processed image strings
     */
    public BatchedImageResponse(List<String> processedImage) {
        this.processedImage = processedImage;
    }

    /**
     * Returns the list of processed base64-encoded images.
     *
     * @return list of image strings
     */
    public List<String> getProcessedImage() {
        return processedImage;
    }
}
