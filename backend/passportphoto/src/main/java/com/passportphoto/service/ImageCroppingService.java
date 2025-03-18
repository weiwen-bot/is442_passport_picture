package com.passportphoto.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;

//Handles all image processing logic (conversion, cropping, encoding).
@Service
public class ImageCroppingService {

    public BufferedImage convertToBufferedImage(MultipartFile imageFile) throws IOException {
        return ImageIO.read(imageFile.getInputStream());
    }

    public BufferedImage cropImage(BufferedImage inputImage, int x, int y, int width, int height) {
        BufferedImage croppedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        croppedImage.getGraphics().drawImage(inputImage, 0, 0, width, height, x, y, x + width, y + height, null);
        return croppedImage;
    }

    public String convertToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}