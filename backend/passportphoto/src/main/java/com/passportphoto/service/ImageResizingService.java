package com.passportphoto.service;

import com.passportphoto.dto.ImageResizeResponse;
import com.passportphoto.service.processor.BackgroundProcessor;
import com.passportphoto.service.processor.TransparentBackgroundProcessor;
import com.passportphoto.service.processor.UniformBackgroundProcessor;
import com.passportphoto.service.strategy.AlphaResizeStrategy;
import com.passportphoto.service.strategy.ResizeStrategy;
import com.passportphoto.service.strategy.StandardResizeStrategy;
import com.passportphoto.util.DimensionHelper;
import com.passportphoto.util.ImageConverter;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
// @Slf4j
public class ImageResizingService {

    // static {
    //     try {
    //         System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    //         System.out.println("OpenCV loaded successfully.");
    //     } catch (UnsatisfiedLinkError e) {
    //         System.err.println("Failed to load OpenCV: " + e.getMessage());
    //     }
    // }

    public String resizeImage(MultipartFile file, String country, String template, Integer customWidth, Integer customHeight) {
        validateInput(file, country, template, customWidth, customHeight);
        try {
            return processImagePipeline(file, country, template, customWidth, customHeight);
        } catch (IOException e) {
            throw new RuntimeException("Image processing failed", e);
        }
    }

    // Pipeline methods
    private String processImagePipeline(MultipartFile file, String country, String template, Integer customWidth, Integer customHeight) throws IOException {
        // 1. Convert uploaded image to BufferedImage
        BufferedImage originalImage = ImageIO.read(file.getInputStream());

        // 2. Detect if input has alpha channel
        boolean hasAlpha = originalImage.getColorModel().hasAlpha();

        // 3. Determine target dimensions
        int[] dimensions = DimensionHelper.getTargetDimensions(country, template, customWidth, customHeight);
        int targetWidth = dimensions[0];
        int targetHeight = dimensions[1];

        // 4. Convert to Mat
        Mat imageMat = ImageConverter.convertBufferedImageToMat(originalImage, hasAlpha);

        // 5. Resize
        ResizeStrategy resizeStrategy = hasAlpha ? new AlphaResizeStrategy() : new StandardResizeStrategy();

        Mat resizedMat = resizeStrategy.resize(imageMat, targetWidth, targetHeight);

        // 6. Process background
        BackgroundProcessor bgProcessor = hasAlpha ? new TransparentBackgroundProcessor() : new UniformBackgroundProcessor();

        Mat finalMat = bgProcessor.process(resizedMat, targetWidth, targetHeight);
        System.out.println(finalMat.width());
        System.out.println(finalMat.height());

        String dataUrl = ImageConverter.convertMatToDataUrl(finalMat);

        // Release resources
        imageMat.release();
        resizedMat.release();
        finalMat.release();

        return dataUrl;
    }

    private void validateInput(MultipartFile file, String country, String template, Integer customWidth, Integer customHeight) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be empty");
        }

        boolean noSizeSpecified = (country == null || country.trim().isEmpty()) &&
                                  (template == null || template.trim().isEmpty()) &&
                                  (customWidth == null || customHeight == null);

        if (noSizeSpecified) {
            throw new IllegalArgumentException("Must specify country, template, or custom dimensions");
        }

        if ((customWidth != null && customWidth <= 0) ||
            (customHeight != null && customHeight <= 0)) {
            throw new IllegalArgumentException("Custom dimensions must be positive integers");
        }
    }
}