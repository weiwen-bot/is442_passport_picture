/*
 * Constants.java
 *
 * This class holds global constants used across the Passport Picture Project.
 * It includes model file names and default configuration values.
 *
 */

package com.passportphoto.util;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

/**
 * The {@code Constants} class contains shared static final values such as
 * file names and default settings used throughout the application.
 */
@Component
public class Constants {

    /** The filename of the ONNX model used for background removal */
    @Value("${model.bgremoval.path}")
    public String MODEL_NAME;

    /** The default background color to use (in hex) */
    @Value("${default.bg.color}")
    public String DEFAULT_BACKGROUND_COLOR;

    /** The Model Multipler for the images to be resized for */
    @Value("${model.size.mutiplier}")
    public int MODEL_SIZE_MULTIPLIER;


    public String getModelName(){
        return MODEL_NAME;
    }
    public String getBackgroundColor(){
        return DEFAULT_BACKGROUND_COLOR;
    }
    public int getModelSizeMultiplier(){
        return MODEL_SIZE_MULTIPLIER;
    }
}
