/*
 * ImageCroppingService.java
 *
 * This service handles image conversion, cropping, and Base64 encoding
 * for use in the Passport Picture Project.
 *
 */

package com.passportphoto.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;

import com.passportphoto.dto.ImageCropRequest;


import com.passportphoto.util.ImageConverterUtil;
import com.passportphoto.util.ValidationUtil;

/**
 * The {@code ImageCroppingService} provides methods for converting uploaded
 * images
 * to {@link BufferedImage}, cropping them, and converting the result to Base64.
 */
@Service
public class ImageCroppingService {

    /**
     * Crops the input image to the specified rectangle.
     *
     * @param inputImage the original image
     * @param x          x-coordinate of the top-left crop corner
     * @param y          y-coordinate of the top-left crop corner
     * @param width      width of the cropped area
     * @param height     height of the cropped area
     * @return the cropped image as a {@link BufferedImage}
     */
    public BufferedImage cropImage(BufferedImage inputImage, int x, int y, int width, int height) {
        BufferedImage croppedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        croppedImage.getGraphics().drawImage(inputImage, 0, 0, width, height, x, y, x + width, y + height, null);
        return croppedImage;
    }

    /**
     * Crops the uploaded image based on provided crop parameters.
     *
     * @param imageFile   the uploaded image
     * @param cropRequest the crop dimensions
     * @return a base64-encoded cropped image
     */
    public String cropFileToImage(MultipartFile imageFile, ImageCropRequest cropRequest) throws Exception {
        ValidationUtil.validateMultipartFile(imageFile);
        BufferedImage inputImage = ImageConverterUtil.convertMultiPartToBufferedImage(imageFile);

        int cropX = (int) cropRequest.getCropX();
        int cropY = (int) cropRequest.getCropY();
        int cropWidth = (int) cropRequest.getCropWidth();
        int cropHeight = (int) cropRequest.getCropHeight();

        // Validate crop dimensions
        ValidationUtil.validateCropDimension(inputImage, cropX, cropY, cropWidth, cropHeight);

        BufferedImage croppedImage = cropImage(inputImage, cropX, cropY, cropWidth, cropHeight);
        String format = imageFile.getContentType().split("/")[1];
        String base64Image = ImageConverterUtil.convertBufferedImgToBase64(croppedImage, format);

        return base64Image;

    }
}