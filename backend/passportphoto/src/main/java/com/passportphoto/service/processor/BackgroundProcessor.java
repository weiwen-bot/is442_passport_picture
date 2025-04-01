/*
 * BackgroundProcessor.java
 *
 * This interface defines the strategy for extending or adjusting
 * image backgrounds to fit specific dimensions.
 *
 */

package com.passportphoto.service.processor;

import org.opencv.core.Mat;

/**
 * The {@code BackgroundProcessor} interface defines a method for extending or processing
 * the background of an image to fit a desired output size.
 */
public interface BackgroundProcessor {
    
    /**
     * Processes the image by extending or modifying the background
     * to match the given target dimensions.
     *
     * @param image        the input image (will not be modified)
     * @param targetWidth  the desired output width
     * @param targetHeight the desired output height
     * @return a new {@link Mat} with the background processed to fit
     */
    Mat process(Mat image, int targetWidth, int targetHeight);
}
