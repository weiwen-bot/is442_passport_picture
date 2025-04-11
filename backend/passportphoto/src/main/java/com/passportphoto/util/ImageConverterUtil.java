/*
 * ImageConverterUtil.java
 *
 * Utility class for converting between Java BufferedImage and OpenCV Mat,
 * and encoding images into base64 Data URLs.
 *
 */

package com.passportphoto.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;

import org.opencv.core.*;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

/**
 * The {@code ImageConverterUtil} class provides static utility methods to:
 * - Convert between {@link BufferedImage} and OpenCV {@link Mat}
 * - Encode images as base64 data URLs
 */
public class ImageConverterUtil {

    /**
     * Converts a Multipart File to a Opencv Mat Object
     *
     * @param file the Multiple File object sent from frontend
     * @return the Mat object of the File
     */
    public static Mat convertFileToMat(MultipartFile file) throws Exception {
        Mat image = Imgcodecs.imdecode(new MatOfByte(file.getBytes()), Imgcodecs.IMREAD_UNCHANGED);
        return image;
    }

    /**
     * Converts an OpenCV Mat to a base64-encoded image data URL.
     * PNG is used for images with alpha; JPG otherwise.
     *
     * @param mat the OpenCV image
     * @return a base64-encoded data URL string
     * @throws IOException if encoding fails
     */
    public static String convertMatToDataUrl(Mat mat) throws IOException {
        BufferedImage image = convertMatToBufferedImage(mat);
        String format = (mat.channels() == 4) ? "png" : "jpg";

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, baos);
            byte[] bytes = baos.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(bytes);
            return "data:image/" + format + ";base64," + base64Image;
        }
    }

    /**
     * Converts a BufferedImage to an OpenCV Mat (BGR or BGRA).
     *
     * @param image    the input BufferedImage
     * @param hasAlpha whether the image has an alpha channel
     * @return an OpenCV Mat representation of the image
     */
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
                    mat.put(y, x, new byte[] { b, g, r, a });
                } else {
                    mat.put(y, x, new byte[] { b, g, r });
                }
            }
        }

        return mat;
    }

    /**
     * Converts an OpenCV Mat to a BufferedImage.
     * Supports 3-channel BGR and 4-channel BGRA images.
     *
     * @param mat the OpenCV Mat to convert
     * @return a BufferedImage representation of the Mat
     */
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
                    int red = (bgra[2] & 0xFF) << 16;
                    int green = (bgra[1] & 0xFF) << 8;
                    int blue = (bgra[0] & 0xFF);

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

    /**
     * Converts a multipart image file into a {@link BufferedImage}.
     *
     * @param imageFile the uploaded multipart file
     * @return the image as a BufferedImage
     * @throws IOException if reading the input stream fails
     */
    public static BufferedImage convertMultiPartToBufferedImage(MultipartFile imageFile) throws IOException {
        return ImageIO.read(imageFile.getInputStream());
    }

    /**
     * Converts a buffered image file into a base64 String.
     *
     * @param imageFile the uploaded multipart file
     * @return the base64 String of Image
     * @throws IOException if reading the input stream fails
     */
    public static String convertBufferedImgToBase64(BufferedImage imageFile, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (imageFile == null) {

        }
        boolean success = ImageIO.write(imageFile, format, baos);
        if (!success) {

            throw new IOException("ImageIO failed to write image with format: " + format);
        }
        byte[] imageBytes = baos.toByteArray();

        // Encode byte array to Base64
        return "data:image/" + format + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * Converts a base64 String into a buffered Image.
     *
     * @param base64Image the base64String of the Image
     * @return the buffered Image of the base64 Image
     * @throws IOException if reading the input stream fails
     */
    public static BufferedImage base64ToBufferedImage(String base64Image) throws IOException {
        BufferedImage image = null;
        try {
            // Remove the Data URI prefix if present
            String base64Data = base64Image.substring(base64Image.indexOf(",") + 1);

            // Decode the Base64 string
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            // Create a ByteArrayInputStream from the decoded bytes
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);

            // Read the image from the InputStream
            image = ImageIO.read(bis);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid Base64 string: " + e.getMessage(), e);
        }
        return image;
    }

    /**
     * Converts a base64 image string to a {@link MultipartFile}.
     *
     * @param base64String the data URL of the image
     * @param fileName     the filename to assign
     * @return a MultipartFile wrapping the decoded image
     * @throws IOException if decoding fails
     */
    public static MultipartFile convertBase64ToMultipartFile(String base64String, String fileName) throws Exception {

        String[] parts = base64String.split(",");
        String mimeType = parts[0].split(":")[1].split(";")[0];
        String base64Data = parts[1];
        byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decodedBytes);

        return new MockMultipartFile(fileName, fileName, mimeType, byteArrayInputStream);
    }

    /**
     * Converts a Mat image to a {@link MultipartFile}.
     *
     * @param base64String the data URL of the image
     * @param fileName     the filename to assign
     * @return a MultipartFile wrapping the decoded image
     * @throws IOException if decoding fails
     */
    public static MultipartFile convertMatToMultipartFile(Mat matImage) throws Exception {

        MatOfByte output = new MatOfByte();
        Imgcodecs.imencode(".jpg", matImage, output);
        byte[] postimageBytes = output.toArray();

        // Create MultipartFile
        MultipartFile multipartFile = new MockMultipartFile(
                "file", // name
                "processed.jpg", // original filename
                "image/jpeg", // content type
                postimageBytes // file content
        );

        return multipartFile;
    }

}
