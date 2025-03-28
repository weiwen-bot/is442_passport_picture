package com.passportphoto.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class ImageConverter {
    public static Mat bufferedImageToMat(BufferedImage image, boolean hasAlpha) {
        return convertBufferedImageToMat(image, hasAlpha);
    }

    public static String matToDataUrl(Mat mat) throws IOException {
        int channels = mat.channels();
        String format = channels == 4 ? "png" : "jpg";
        BufferedImage outputImage = convertMatToBufferedImage(mat);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(outputImage, format, baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return "data:image/" + format + ";base64," + base64;
        } 
    }

    /* BUFFERED IMAGE <-> OPENCV MAT */

    public static Mat convertBufferedImageToMat(BufferedImage image, boolean hasAlpha) {
        int width = image.getWidth();
        int height = image.getHeight();
        int type = hasAlpha ? CvType.CV_8UC4 : CvType.CV_8UC3;
        Mat mat = new Mat(height, width, type);
    
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
    
                byte r = (byte) ((pixel >> 16) & 0xFF);
                byte g = (byte) ((pixel >> 8) & 0xFF);
                byte b = (byte) (pixel & 0xFF);
    
                if (hasAlpha) {
                    byte a = (byte) ((pixel >> 24) & 0xFF);
                    mat.put(y, x, new byte[]{b, g, r, a}); // OpenCV expects BGRA
                } else {
                    mat.put(y, x, new byte[]{b, g, r}); // OpenCV expects BGR
                }
            }
        }
    
        return mat;
    }

    public static BufferedImage convertMatToBufferedImage(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        BufferedImage image;

        if (channels == 4) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
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
        } else if (channels == 3) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            byte[] bgr = new byte[3];
            for (int y = 0; y < mat.rows(); y++) {
                for (int x = 0; x < mat.cols(); x++) {
                    mat.get(y, x, bgr);
                    
                    int rgb = (bgr[2] & 0xFF) << 16 | (bgr[1] & 0xFF) << 8 | (bgr[0] & 0xFF);
                    image.setRGB(x, y, rgb);
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported number of channels: " + channels);
        }

        return image;
    }

    // Convert Mat to base64 URL
    public static String convertMatToDataUrl(Mat mat) throws IOException {
        BufferedImage image = convertMatToBufferedImage(mat);
        // 8. Encode to Base64.
            //    - If RGBA, better use PNG to preserve transparency.
            //    - If BGR only, JPG is fine.
        String format = (mat.channels() == 4) ? "png" : "jpg";

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, baos);
            byte[] bytes = baos.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(bytes);
            return "data:image/" + format + ";base64," + base64Image;
        }
    }
}
