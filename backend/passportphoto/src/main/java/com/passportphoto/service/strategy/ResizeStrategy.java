/*
 * ResizeStrategy.java
 *
 * This interface defines the strategy for resizing an image to
 * a specific target width and height using OpenCV.
 *
 */

package com.passportphoto.service.strategy;

import org.opencv.core.Mat;

/**
 * The {@code ResizeStrategy} interface defines a method for resizing
 * an image represented as an OpenCV {@link Mat} to a specified size.
 */
public interface ResizeStrategy {

    /**
     * Resizes the input image to the given width and height.
     *
     * @param image        the original OpenCV image matrix
     * @param targetWidth  the desired width of the resized image
     * @param targetHeight the desired height of the resized image
     * @return a new {@code Mat} object representing the resized image
     */
    Mat resize(Mat image, int targetWidth, int targetHeight);
}