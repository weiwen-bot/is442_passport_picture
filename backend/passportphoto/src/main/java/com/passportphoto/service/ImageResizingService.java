package com.passportphoto.service;

import com.passportphoto.dto.ImageResizeResponse;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class ImageResizingService {
    static {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            System.out.println("OpenCV loaded successfully.");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load OpenCV: " + e.getMessage());
        }
    }

    public ImageResizeResponse resizeImage(MultipartFile file, String country, String template, Integer customWidth, Integer customHeight) {
        try {
            // 1. Convert uploaded image to BufferedImage
            BufferedImage originalImage = ImageIO.read(file.getInputStream());

            // 2. Detect if input has alpha channel
            boolean hasAlpha = originalImage.getColorModel().hasAlpha();

            // 3. Convert to Mat (BGR or BGRA)
            Mat imageMat = hasAlpha? bufferedImageToMatRGBA(originalImage): bufferedImageToMatBGR(originalImage);

            // 4. Determine target dimensions
            int targetWidth = 0, targetHeight = 0;
            if (country != null && !country.isEmpty()) {
                int[] dimensions = getPassportPhotoDimensions(country);
                targetWidth = dimensions[0];
                targetHeight = dimensions[1];
            } else if (template != null && !template.isEmpty()) {
                int[] dimensions = getTemplateDimensions(template);
                targetWidth = dimensions[0];
                targetHeight = dimensions[1];
            } else if (customWidth != null && customHeight != null) {
                targetWidth = customWidth;
                targetHeight = customHeight;
            } else {
                throw new IllegalArgumentException("Invalid resize request: No valid country, template, or custom size provided.");
            }

            // 5. Resize the image to fit the target dimensions
            Mat resized;
            if (hasAlpha) {
                // 4-channel path
                resized = resizeWithAlpha(imageMat, targetWidth, targetHeight);
            } else {
                // 3-channel path
                resized = resizeToTargetBGR(imageMat, targetWidth, targetHeight);
            }

            // 6. Extend background if needed
            Mat finalMat;
            if (hasAlpha) {
                // For RGBA, we keep extension transparent.
                finalMat = extendTransparentBackground(resized, targetWidth, targetHeight);
            } else {
                // For BGR, we either replicate/blur or do uniform fill 
                // depending on whether the background is uniform.
                finalMat = extendBackgroundBGR(resized, targetWidth, targetHeight);
            }

            // 7. Convert final Mat back to BufferedImage
            BufferedImage outputImage;
            if (finalMat.channels() == 4) {
                // RGBA => use a method that preserves alpha
                outputImage = matToBufferedImageRGBA(finalMat);
            } else {
                // BGR => normal 3-channel
                outputImage = matToBufferedImageBGR(finalMat);
            }

            // 8. Encode to Base64.
            //    - If RGBA, better use PNG to preserve transparency.
            //    - If BGR only, JPG is fine.
            String format = (finalMat.channels() == 4) ? "png" : "jpg";
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(outputImage, format, baos);
            byte[] bytes = baos.toByteArray();
            baos.close();

            String base64Image = Base64.getEncoder().encodeToString(bytes);
            String mimeType = (finalMat.channels() == 4) ? "image/png" : "image/jpeg";
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;

            return new ImageResizeResponse("success", "Image resized successfully", dataUrl);
        } catch (IOException e) {
            throw new RuntimeException("Image resize failed", e);
        }
    }

    // Method to maintain aspect ratio with FIT strategy
    private Size calculateFitSize(Size originalSize, int targetWidth, int targetHeight) {
        double originalAspect = (double) originalSize.width / originalSize.height;
        double targetAspect = (double) targetWidth / targetHeight;
    
        int newWidth, newHeight;
        if (originalAspect > targetAspect) {
            // Image is relatively wider -> match target's width
            newWidth = targetWidth;
            newHeight = (int) Math.round(targetWidth / originalAspect);
        } else {
            // Image is relatively taller (or equal) -> match target's height
            newHeight = targetHeight;
            newWidth = (int) Math.round(targetHeight * originalAspect);
        }
        return new Size(newWidth, newHeight);
    }
    
    /* RESIZING 3-CHANNEL vs 4-CHANNEL */

    // Method to resize RGBA image and returns CV_8UC4
    private Mat resizeWithAlpha(Mat rgbaImage, int targetWidth, int targetHeight) {
        int w = rgbaImage.width();
        int h = rgbaImage.height();
    
        // 1) If bigger => downscale using calculateFitSize()
        if (w > targetWidth || h > targetHeight) {
            Size newSize = calculateFitSize(rgbaImage.size(), targetWidth, targetHeight);
            Mat resized = new Mat();
    
            // Downscale => INTER_AREA
            Imgproc.resize(rgbaImage, resized, newSize, 0, 0, Imgproc.INTER_AREA);
            return resized;
        }
    
        // 2) If smaller => upscale (but do NOT exceed target)
        if (w < targetWidth || h < targetHeight) {
            double scaleX = (double) targetWidth / w;
            double scaleY = (double) targetHeight / h;
            double scale = Math.min(scaleX, scaleY);
    
            int newW = (int) Math.round(w * scale);
            int newH = (int) Math.round(h * scale);
            Size newSize = new Size(newW, newH);
            Mat resized = new Mat();
    
            // Upscale => INTER_CUBIC
            Imgproc.resize(rgbaImage, resized, newSize, 0, 0, Imgproc.INTER_CUBIC);
            return resized;
        }
    
        // 3) If it already fits within target dims => no resize
        return rgbaImage;
    }

    // Method to resise BGR image
    private Mat resizeToTargetBGR(Mat bgrImage, int targetWidth, int targetHeight) {
        int w = bgrImage.width();
        int h = bgrImage.height();
    
        // 1) If bigger in either dimension => "fit" using calculateFitSize() => scale down
        if (w > targetWidth || h > targetHeight) {
            Size newSize = calculateFitSize(bgrImage.size(), targetWidth, targetHeight);
            Mat resized = new Mat();
    
            // Scaling down ==> so INTER_AREA is preferred
            int interpolation = Imgproc.INTER_AREA;
            Imgproc.resize(bgrImage, resized, newSize, 0, 0, interpolation);
            return resized;
        }
    
        // 2) If smaller in either dimension => scale up (but do NOT exceed target)
        if (w < targetWidth || h < targetHeight) {
            double scaleX = (double) targetWidth / w;
            double scaleY = (double) targetHeight / h;
            double scale = Math.min(scaleX, scaleY); 
    
            int newW = (int) Math.round(w * scale);
            int newH = (int) Math.round(h * scale);
    
            Size newSize = new Size(newW, newH);
            Mat resized = new Mat();
    
            // Scaling up ==> so INTER_CUBIC is typically higher quality
            int interpolation = Imgproc.INTER_CUBIC;
            Imgproc.resize(bgrImage, resized, newSize, 0, 0, interpolation);
            return resized;
        }
    
        // 3) If already within targetWidth x targetHeight, no resize is needed
        return bgrImage;
    }
    
    /* BACKGROUND EXTENSION */
    
    // Method to extend canvas with transparency if image is RGBA
    private Mat extendTransparentBackground(Mat rgbaImage, int targetWidth, int targetHeight) {
        // 1) If already the same size, nothing to do
        if (rgbaImage.width() == targetWidth && rgbaImage.height() == targetHeight) {
            return rgbaImage.clone();
        }

        // 2) Compute offsets
        int xOffset = (int)Math.round((targetWidth - rgbaImage.width()) / 2.0);
        int yOffset = (int)Math.round((targetHeight - rgbaImage.height()) / 2.0);

        // 3) Create new transparent canvas
        Mat extended = new Mat(targetHeight, targetWidth, CvType.CV_8UC4, new Scalar(0,0,0,0));

        // 4) Copy the resized image into the center
        Rect roi = new Rect(xOffset, yOffset, rgbaImage.width(), rgbaImage.height());
        Mat center = extended.submat(roi);
        rgbaImage.copyTo(center);

        return extended;
    }

    /* For a BGR image, we either:
        1. Detect if the background is uniform, and just fill the extension with that colour
        2. Use replicate + blur if background is not uniform
    */
    private Mat extendBackgroundBGR(Mat bgrImage, int targetWidth, int targetHeight) {
        if (bgrImage.width() == targetWidth && bgrImage.height() == targetHeight) {
            return bgrImage.clone();
        }

        // Compute stats from border region
        BorderStats stats = computeBorderStats(bgrImage, 10 /*borderSize*/, 15.0 /*stdDevThreshold*/);

        if (stats.isUniform) {
            // Extend with the cornerColor
            return extendBackgroundUniform(bgrImage, targetWidth, targetHeight, stats.meanColor);
        } else {
            // Use replicate + blur
            return extendBackgroundReplicate(bgrImage, targetWidth, targetHeight);
        }
    }

    // Method to extend canvas with uniform colour
    private Mat extendBackgroundUniform(Mat image, int targetWidth, int targetHeight, Scalar bgColor) {
        // 1) If image already matches target, just return
        if (image.width() == targetWidth && image.height() == targetHeight) {
            return image.clone();
        }
        // 2) Create offsets for centering
        int xOffset = (int) Math.round((targetWidth - image.width()) / 2.0);
        int yOffset = (int) Math.round((targetHeight - image.height()) / 2.0);

        // 3) Create a new canvas
        Mat extendedMat = new Mat(targetHeight, targetWidth, image.type());
        // Fill with the uniform color
        extendedMat.setTo(bgColor);

        // 4) Copy the main image into the center
        Rect roi = new Rect(xOffset, yOffset, image.width(), image.height());
        Mat centerRoi = extendedMat.submat(roi);
        image.copyTo(centerRoi);

        return extendedMat;
    }

    // Method to extend background - using OpenCV (replicate + blur)
    private Mat extendBackgroundReplicate(Mat image, int targetWidth, int targetHeight) {
    
        // 1. Create offsets for centering
        int xOffset = (int) Math.round((targetWidth - image.width()) / 2.0);
        int yOffset = (int) Math.round((targetHeight - image.height()) / 2.0);
    
        // 2. Create a larger canvas if needed
        Mat extendedMat = new Mat(targetHeight, targetWidth, image.type());
        // Fill with black
        extendedMat.setTo(new Scalar(0,0,0));
    
        // 3. Copy the main image into the center of extendedMat
        Rect roi = new Rect(xOffset, yOffset, image.width(), image.height());
        Mat centerRoi = extendedMat.submat(roi);
        image.copyTo(centerRoi);
    
        // 4. Extract the center area to a temporary Mat, call copyMakeBorder
        Mat justCenter = new Mat();
        // Copy just the center part from extendedMat
        extendedMat.submat(roi).copyTo(justCenter);
    
        // 5. Compute border sizes
        int top = yOffset;
        int bottom = targetHeight - image.height() - yOffset;
        int left = xOffset;
        int right = targetWidth - image.width() - xOffset;
    
        // 6. copyMakeBorder with replicate
        Mat borderReplicated = new Mat();
        Core.copyMakeBorder(
            justCenter,
            borderReplicated,
            top, bottom, left, right,
            Core.BORDER_REPLICATE
        );
    
        // 7. Light blur only the newly created border region
        Rect innerROI = new Rect(xOffset, yOffset, image.width(), image.height());
        Mat blurred = applySoftBlurToBorder(borderReplicated, innerROI);

        // Clean up
        justCenter.release();
        return blurred;
    }
    
    // Method to do a small blur outside the border region
    private Mat applySoftBlurToBorder(Mat extendedMat, Rect roi) {
    
        // 1. Duplicate the entire extendedMat
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(extendedMat, blurred, new Size(9, 9), 0); 
    
        // 2. Create a mask where the ROI is black (unblurred) and the rest is white (blurred)
        Mat mask = Mat.zeros(extendedMat.size(), CvType.CV_8UC1);
        mask.setTo(new Scalar(255));
        Imgproc.rectangle(mask, roi, new Scalar(0), -1);
    
        // 3. Copy from 'blurred' into 'extendedMat' only where mask=255
        blurred.copyTo(extendedMat, mask);
    
        blurred.release();
        mask.release();
        return extendedMat;
    }

    /* UNIFORM BACKGROUND DETECTION 
     * Gather pixels from a 10px-wide band around all edges, then compute mean/stddev
     * If the stddev is below a threshold, consider it uniform
    */
    private BorderStats computeBorderStats(Mat bgrImage, int borderSize, double stdDevThreshold) {
        BorderStats result = new BorderStats();

        // 1) Extract perimeter region (top/bottom/left/right)
        Mat borderRegion = extractBorderRegion(bgrImage, borderSize);
        if (borderRegion.empty()) {
            // fallback: either treat as uniform or return something default
            result.isUniform = true;
            result.meanColor = new Scalar(128,128,128); // middle gray
            return result;
        }
    
        // 2) Compute mean & stddev
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(borderRegion, mean, stddev);
    
        double stdB = stddev.get(0,0)[0];
        double stdG = stddev.get(1,0)[0];
        double stdR = stddev.get(2,0)[0];
    
        // 3) If all channels have std < threshold => uniform
        result.isUniform = (stdB < stdDevThreshold && stdG < stdDevThreshold && stdR < stdDevThreshold);
    
        double meanB = mean.get(0,0)[0];
        double meanG = mean.get(1,0)[0];
        double meanR = mean.get(2,0)[0];
        result.meanColor = new Scalar(meanB, meanG, meanR);
    
        return result;
    }
    
    private Mat extractBorderRegion(Mat bgrImage, int borderSize) {
        int w = bgrImage.cols();
        int h = bgrImage.rows();
        if (w < borderSize*2 || h < borderSize*2) {
            // If image is too small, no perimeter band to extract
            return new Mat(); // empty
        }

        // 1. Create submats for each edge
        Mat top    = bgrImage.submat(0, borderSize, 0, w); 
        Mat bottom = bgrImage.submat(h-borderSize, h, 0, w);
        Mat left   = bgrImage.submat(borderSize, h-borderSize, 0, borderSize);
        Mat right  = bgrImage.submat(borderSize, h-borderSize, w-borderSize, w);

        // 2. Calculate total number of border pixels
        int topPixels = top.rows() * top.cols();
        int bottomPixels = bottom.rows() * bottom.cols();
        int leftPixels = left.rows() * left.cols();
        int rightPixels = right.rows() * right.cols();
        int totalPixels = topPixels + bottomPixels + leftPixels + rightPixels;

        // 3. Create a single-column mat of the same type, which will hold all these pixels
        Mat borderRegion = new Mat(totalPixels, 1, bgrImage.type());

        // 4. Copy each submat's pixels into 'borderRegion'
        int currentIndex = 0;
        copySubmatToColumn(top, borderRegion, currentIndex);
        currentIndex += topPixels;

        copySubmatToColumn(bottom, borderRegion, currentIndex);
        currentIndex += bottomPixels;

        copySubmatToColumn(left, borderRegion, currentIndex);
        currentIndex += leftPixels;

        copySubmatToColumn(right, borderRegion, currentIndex);

        return borderRegion;
    }

    private void copySubmatToColumn(Mat region, Mat dest, int startIndex) {
        int rows = region.rows();
        int cols = region.cols();
    
        // Read each pixel in 'region' and store it in 'dest' at row [startIndex + ...], col=0
        byte[] pixel = new byte[(int) region.elemSize()]; 
    
        int index = startIndex; 
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                region.get(y, x, pixel);
                dest.put(index, 0, pixel);
                index++;
            }
        }
    }

    static class BorderStats {
        boolean isUniform;
        Scalar meanColor;
    }

    /* BUFFERED IMAGE <-> OPENCV MAT */

    // Method to convert BufferedImage (no alpha) to OpenCV Mat
    private Mat bufferedImageToMatBGR(BufferedImage image) {
        // Create a new Mat of the right size
        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
    
        // Get RGB data from the image by directly sampling pixels
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                byte b = (byte) (rgb & 0xFF);
                byte g = (byte) ((rgb >> 8) & 0xFF);
                byte r = (byte) ((rgb >> 16) & 0xFF);

                mat.put(y, x, new byte[]{b, g, r});
            }
        }
    
        return mat;
    }

    // Method to convert BufferedImage (with alpha) to OpenCV Mat
    private Mat bufferedImageToMatRGBA(BufferedImage image) {
        // Create a new Mat of size (height x width) with 4 channels
        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC4);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgba = image.getRGB(x, y);

                byte a = (byte) ((rgba >> 24) & 0xFF);
                byte r = (byte) ((rgba >> 16) & 0xFF);
                byte g = (byte) ((rgba >> 8) & 0xFF);
                byte b = (byte) (rgba & 0xFF);

                // OpenCV expects BGRA
                mat.put(y, x, new byte[]{b, g, r, a});
            }
        }
        return mat;
    }

    // Method to convert 3-channel BGR Mat back to BufferedImage
    private BufferedImage matToBufferedImageBGR(Mat mat) {
        // Create a BufferedImage
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_3BYTE_BGR);
    
        // Get each pixel from the Mat and set it in the BufferedImage
        byte[] bgr = new byte[3];
        for (int y = 0; y < mat.rows(); y++) {
            for (int x = 0; x < mat.cols(); x++) {
                mat.get(y, x, bgr);
            
                // Convert from BGR to RGB for Java
                int rgb = (bgr[2] & 0xFF) << 16 | (bgr[1] & 0xFF) << 8 | (bgr[0] & 0xFF);
                image.setRGB(x, y, rgb);
            }
        }
    
        return image;
    }

    // Method to convert 4-channel BGRA Mat into a BufferedImage
    private BufferedImage matToBufferedImageRGBA(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

        byte[] bgra = new byte[4];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                mat.get(y, x, bgra);
                int alpha = (bgra[3] & 0xFF) << 24;
                int red   = (bgra[2] & 0xFF) << 16;
                int green = (bgra[1] & 0xFF) << 8;
                int blue  = (bgra[0] & 0xFF);

                int rgba = alpha | red | green | blue;
                image.setRGB(x, y, rgba);
            }
        }
        return image;
    }

    // Method to get passport photo dimensions based on the selected country
    // pixels = size in mm / 25.4 * DPI
    private int[] getPassportPhotoDimensions(String country) {
        if (country == null || country.trim().isEmpty()) {
            System.out.println("Country input is null or empty, defaulting to 413x531");
            return new int[]{413, 531}; // Default size
        }

        country = country.trim().toLowerCase();
        System.out.println("Received country: [" + country + "]");

        switch (country.toLowerCase()) {
            case "jpn":
                return new int[]{413, 531}; // 35mm x 45mm at 300 dpi
            case "usa":
                return new int[]{602, 602}; // 51mm x 51mm at 300 dpi
            case "sgp":
                return new int[]{413, 531}; // 35mm x 45mm at 300 dpi
            case "chn":
                return new int[]{390, 567}; // 33mm x 48mmm at 300 dpi
            case "mas":
                return new int[]{413, 591}; // 35mm x 50 mm at 300 dpi
            default:
                System.out.println("Country not found, defaulting to 413x531");
                return new int[]{413, 531}; // Default to JPN/SGP size
        }
    }

    // Method to get standard template dimensions based on template name
    private int[] getTemplateDimensions(String template) {
        Map<String, int[]> templateMap = new HashMap<>();
        templateMap.put("2R", new int[]{600, 900});
        templateMap.put("3R", new int[]{1050, 1500});
        templateMap.put("4R", new int[]{1200, 1800});
        templateMap.put("Instagram Post", new int[]{1080, 1080});
        templateMap.put("Instagram Story", new int[]{1080, 1920});
        templateMap.put("YouTube Thumbnail", new int[]{1280, 720});
        templateMap.put("HD Wallpaper", new int[]{1920, 1080});
        templateMap.put("4K Wallpaper", new int[]{3840, 2160});

        return templateMap.getOrDefault(template, new int[]{600, 900}); // Default to 2R size
    }
}