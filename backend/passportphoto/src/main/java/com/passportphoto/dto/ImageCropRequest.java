/*
 * ImageCropRequest.java
 * 
 * This class represents the request DTO for the image cropping operation
 * in the Passport Picture Project.
 * 
 */

package com.passportphoto.dto;

/**
 * The {@code ImageCropRequest} class is a Data Transfer Object (DTO)
 * that contains crop parameters for image manipulation.
 */

public class ImageCropRequest {

    /** The x-coordinate of the top-left crop starting point */
    private double cropX;

    /** The y-coordinate of the top-left crop starting point */
    private double cropY;

    /** The width of the crop area */
    private double cropWidth;

    /** The height of the crop area */
    private double cropHeight;

    /**
     * Gets the x-coordinate of the crop.
     *
     * @return the crop x-coordinate
     */
    public double getCropX() { 
        return cropX; 
    }

    /**
     * Sets the x-coordinate of the crop.
     *
     * @param cropX the x-coordinate to set
     */
    public void setCropX(double cropX) { 
        this.cropX = cropX; 
    }

    /**
     * Gets the y-coordinate of the crop.
     *
     * @return the crop y-coordinate
     */
    public double getCropY() { 
        return cropY; 
    }

    /**
     * Sets the y-coordinate of the crop.
     *
     * @param cropY the y-coordinate to set
     */
    public void setCropY(double cropY) { 
        this.cropY = cropY; 
    }

    /**
     * Gets the crop width.
     *
     * @return the width of the crop area
     */
    public double getCropWidth() { 
        return cropWidth; 
    }

     /**
     * Sets the crop width.
     *
     * @param cropWidth the width to set
     */
    public void setCropWidth(double cropWidth) { 
        this.cropWidth = cropWidth; 
    }

    /**
     * Gets the crop height.
     *
     * @return the height of the crop area
     */
    public double getCropHeight() { 
        return cropHeight; 
    }

    /**
     * Sets the crop height.
     *
     * @param cropHeight the height to set
     */
    public void setCropHeight(double cropHeight) { 
        this.cropHeight = cropHeight; 
    }
}
