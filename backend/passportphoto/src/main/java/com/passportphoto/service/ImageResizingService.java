/*
 * ImageResizingService.java
 *
 * This service handles the full image resizing pipeline, including conversion,
 * resizing using different strategies, and background processing based on alpha presence.
 *
 */

package com.passportphoto.service;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.opencv.core.Mat;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.passportphoto.service.processor.BackgroundProcessor;
import com.passportphoto.service.processor.TransparentBackgroundProcessor;
import com.passportphoto.service.processor.UniformBackgroundProcessor;
import com.passportphoto.service.strategy.AlphaResizeStrategy;
import com.passportphoto.service.strategy.ResizeStrategy;
import com.passportphoto.service.strategy.StandardResizeStrategy;
import com.passportphoto.util.DimensionHelper;
import com.passportphoto.util.ImageConverter;

/**
 * The {@code ImageResizingService} provides an end-to-end pipeline for
 * resizing and formatting an uploaded image according to the target dimensions
 * and image type (with or without alpha transparency).
 */
@Service
public class ImageResizingService {

    /**
     * Resizes an uploaded image based on country, template, or custom dimensions.
     *
     * @param file          the uploaded image file
     * @param country       the selected country code (optional)
     * @param template      the selected template name (optional)
     * @param customWidth   the custom width (optional)
     * @param customHeight  the custom height (optional)
     * @return a base64 data URL of the resized and processed image
     */
    public String resizeImage(MultipartFile file, String country, String template, Integer customWidth, Integer customHeight) {
        validateInput(file, country, template, customWidth, customHeight);
        try {
            return processImagePipeline(file, country, template, customWidth, customHeight);
        } catch (IOException e) {
            throw new RuntimeException("Image processing failed", e);
        }
    }

    /**
     * Executes the image processing pipeline for resizing and formatting the image.
     * Steps: Convert -> Detect Alpha -> Determine Size -> Resize -> Extend Background -> Encode
     * 
     * @return a base64 string representing the final resized image
     */
    private String processImagePipeline(MultipartFile file, String country, String template, Integer customWidth, Integer customHeight) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        boolean hasAlpha = originalImage.getColorModel().hasAlpha();

        int[] dimensions = DimensionHelper.getTargetDimensions(country, template, customWidth, customHeight);
        int targetWidth = dimensions[0];
        int targetHeight = dimensions[1];

        Mat imageMat = ImageConverter.convertBufferedImageToMat(originalImage, hasAlpha);

        ResizeStrategy resizeStrategy = hasAlpha ? new AlphaResizeStrategy() : new StandardResizeStrategy();

        Mat resizedMat = resizeStrategy.resize(imageMat, targetWidth, targetHeight);

        BackgroundProcessor bgProcessor = hasAlpha ? new TransparentBackgroundProcessor() : new UniformBackgroundProcessor();

        Mat finalMat = bgProcessor.process(resizedMat, targetWidth, targetHeight);
        String dataUrl = ImageConverter.convertMatToDataUrl(finalMat);

        imageMat.release();
        resizedMat.release();
        finalMat.release();

        return dataUrl;
    }

    /**
     * Validates user input: uploaded file presence and dimension selection.
     *
     * @throws IllegalArgumentException if validation fails
     */
    private void validateInput(MultipartFile file, String country, String template, Integer customWidth, Integer customHeight) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be empty");
        }

        boolean noSizeSpecified = (country == null || country.trim().isEmpty()) && (template == null || template.trim().isEmpty()) && (customWidth == null || customHeight == null);

        if (noSizeSpecified) {
            throw new IllegalArgumentException("Must specify country, template, or custom dimensions");
        }

        if ((customWidth != null && customWidth <= 0) ||
            (customHeight != null && customHeight <= 0)) {
            throw new IllegalArgumentException("Custom dimensions must be positive integers");
        }
    }
}