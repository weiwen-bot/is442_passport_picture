package com.passportphoto.util;

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.opencv.core.*;
import org.springframework.web.multipart.MultipartFile;

import com.passportphoto.exceptions.ImageInvalidFormatException;

public class ValidationUtil {

    // File Validation
    public static void validateMultipartFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded image file is null or empty.");
        }
    }

    // Image Validation
    public static void validateMatImage(Mat image) throws Exception {
       if (image.empty()) {
            throw new IllegalArgumentException("Failed to load image!");
        }
    }

    // Crop Dimension Validation
    public static void validateCropDimension(BufferedImage inputImage, int cropX, int cropY, int cropWidth, int cropHeight) throws Exception{
        if (cropX < 0 || cropY < 0 || cropWidth <= 0 || cropHeight <= 0 ||
                cropX + cropWidth > inputImage.getWidth() ||
                cropY + cropHeight > inputImage.getHeight()) {
            throw new IndexOutOfBoundsException("Invalid crop dimensions!");
        }

    }

    // BufferedImage Validation
    public static void validateBufferedImage(BufferedImage image, int width, int height){
        if (image == null || width < 0 || height < 0){
            throw new ImageInvalidFormatException("Invalid image format");
        }

    }


    
}
