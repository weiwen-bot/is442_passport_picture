package com.passportphoto.controller;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.awt.Graphics2D;

@RestController
@RequestMapping("/image")
@CrossOrigin(origins = "http://localhost:5173")

public class ImageResizingController {
    static {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            System.out.println("OpenCV loaded successfully.");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load OpenCV: " + e.getMessage());
        }
    }

    @PostMapping("/resize")
    public ResponseEntity<String> resizeImage(@RequestParam("image") MultipartFile file, @RequestParam("country") String country) {
        try {
            // 1. Convert uploaded image to BufferedImage
            BufferedImage originalImage = ImageIO.read(file.getInputStream());

            // 2. Detect if input has alpha channel
            boolean hasAlpha = originalImage.getColorModel().hasAlpha();

            // 3. Convert to Mat (BGR or BGRA)
            Mat imageMat = hasAlpha? bufferedImageToMatRGBA(originalImage): bufferedImageToMatBGR(originalImage);

            // 4. Get target passport size
            int[] dimensions = getPassportPhotoDimensions(country);
            int targetWidth = dimensions[0];
            int targetHeight = dimensions[1];

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

            return ResponseEntity.ok("{\"status\":\"success\", \"message\":\"Image resized successfully\", \"image\":\"" + dataUrl + "\"}");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("{\"status\":\"error\", \"message\":\"Image resize failed\"}");
        }
    }

    // Method to maintain aspect ratio with FIT strategy
    private Size calculateFitSize(Size originalSize, int targetWidth, int targetHeight) {
        double originalAspect = originalSize.width / originalSize.height;
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
        Size newSize = calculateFitSize(rgbaImage.size(), targetWidth, targetHeight);
        Mat resizedRgba = new Mat();
        Imgproc.resize(rgbaImage, resizedRgba, newSize, 0, 0, Imgproc.INTER_AREA);
        return resizedRgba;
    }

    // Method to resise BGR image
    private Mat resizeToTargetBGR(Mat bgrImage, int targetWidth, int targetHeight) {
        int w = bgrImage.width();
        int h = bgrImage.height();
    
        // 1) If bigger in either dimension => "fit" using calculateFitSize()
        if (w > targetWidth || h > targetHeight) {
            Size newSize = calculateFitSize(bgrImage.size(), targetWidth, targetHeight);
            Mat resized = new Mat();
            Imgproc.resize(bgrImage, resized, newSize, 0, 0, Imgproc.INTER_AREA);
            return resized;
        }
    
        // 2) If smaller in either dimension => scale up (but do NOT exceed target)
        if (w < targetWidth || h < targetHeight) {
            double scaleX = (double) targetWidth / w;
            double scaleY = (double) targetHeight / h;
            // Use min so we don't exceed the target in the other dimension
            double scale = Math.min(scaleX, scaleY);
    
            int newW = (int) Math.round(w * scale);
            int newH = (int) Math.round(h * scale);
    
            Mat resized = new Mat();
            Imgproc.resize(bgrImage, resized, new Size(newW, newH), 0, 0, Imgproc.INTER_AREA);
            return resized;
        }
    
        // 3) If it’s already within targetWidth x targetHeight => no resize
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

        // Quick check for uniform background by sampling corners:
        Scalar cornerColor = sampleCornerColor(bgrImage);
        boolean isUniform = checkIfBackgroundIsUniform(bgrImage, cornerColor);

        if (isUniform) {
            // Extend with the cornerColor
            return extendBackgroundUniform(bgrImage, targetWidth, targetHeight, cornerColor);
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

    /* UNIFORM BACKGROUND DETECTION */
    private Scalar sampleCornerColor(Mat bgrImage) {
        // Sample the top-left corner colour
        double[] cornerPix = bgrImage.get(0, 0);
        return new Scalar(cornerPix);
    }
    
    // Sample corners and compare them to cornerColour
    private boolean checkIfBackgroundIsUniform(Mat bgrImage, Scalar cornerColor) {
        int height = bgrImage.rows();
        int width = bgrImage.cols();

        // Sample the four corners
        Scalar c1 = new Scalar(bgrImage.get(0, 0));
        Scalar c2 = new Scalar(bgrImage.get(0, width - 1));
        Scalar c3 = new Scalar(bgrImage.get(height - 1, 0));
        Scalar c4 = new Scalar(bgrImage.get(height - 1, width - 1));

        // Use a small threshold
        double threshold = 15.0;

        return (colorDistance(c1, cornerColor) < threshold
             && colorDistance(c2, cornerColor) < threshold
             && colorDistance(c3, cornerColor) < threshold
             && colorDistance(c4, cornerColor) < threshold);
    }

    private double colorDistance(Scalar a, Scalar b) {
        // Euclidean distance in BGR space
        double db = a.val[0] - b.val[0];
        double dg = a.val[1] - b.val[1];
        double dr = a.val[2] - b.val[2];
        return Math.sqrt(db*db + dg*dg + dr*dr);
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
}
