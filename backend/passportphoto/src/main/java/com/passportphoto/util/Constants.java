/*
 * Constants.java
 *
 * This class holds global constants used across the Passport Picture Project.
 * It includes model file names and default configuration values.
 *
 */

package com.passportphoto.util;

/**
 * The {@code Constants} class contains shared static final values such as
 * file names and default settings used throughout the application.
 */
public class Constants {

    /** The filename of the ONNX model used for background removal */
    public static final String MODEL_NAME = "modnet.onnx";

    /** The default background color to use (in hex) */
    public static final String DEFAULT_BACKGROUND_COLOR = "#FFFFFF";

    /** The Model Multipler for the images to be resized for */
    public static final int MODEL_SIZE_MULTIPLIER = 32;
}
