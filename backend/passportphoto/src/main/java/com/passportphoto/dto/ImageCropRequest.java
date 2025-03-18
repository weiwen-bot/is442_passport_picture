package com.passportphoto.dto;

// Handle DTO for structured crop request parameters.
public class ImageCropRequest {
    private double cropX;
    private double cropY;
    private double cropWidth;
    private double cropHeight;

    public double getCropX() { 
        return cropX; 
    }
    public void setCropX(double cropX) { 
        this.cropX = cropX; 
    }

    public double getCropY() { 
        return cropY; 
    }
    public void setCropY(double cropY) { 
        this.cropY = cropY; 
    }

    public double getCropWidth() { 
        return cropWidth; 
    }
    public void setCropWidth(double cropWidth) { 
        this.cropWidth = cropWidth; 
    }

    public double getCropHeight() { 
        return cropHeight; 
    }
    public void setCropHeight(double cropHeight) { 
        this.cropHeight = cropHeight; 
    }
}
