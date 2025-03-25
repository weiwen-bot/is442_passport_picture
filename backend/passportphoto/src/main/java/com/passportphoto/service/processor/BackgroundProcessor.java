package com.passportphoto.service.processor;

import org.opencv.core.Mat;

public interface BackgroundProcessor {
    /**
     * Extends the image to the target dimensions while handling background appropriately
     * @param image Input image (will not be modified)
     * @param targetWidth Desired output width
     * @param targetHeight Desired output height
     * @return New Mat with processed background
     */
    Mat process(Mat image, int targetWidth, int targetHeight);
}
