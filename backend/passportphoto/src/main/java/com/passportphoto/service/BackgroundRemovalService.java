/*
 * BackgroundRemovalService.java
 *
 * This service handles background removal using an ONNX model (MODNet),
 * supporting pre-processing, ONNX inference, and post-processing with
 * blending over a solid color or custom image background.
 *
 */

package com.passportphoto.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Base64;
import java.util.Collections;


import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.passportphoto.exceptions.ImageInvalidFormatException;
import com.passportphoto.util.Constants;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

/**
 * The {@code BackgroundRemovalService} handles the pipeline for removing
 * backgrounds from passport images using a MODNet ONNX model.
 */
@Service
public class BackgroundRemovalService {

	private final ModelSessionManager modelSessionManager;
	private final ImageProcessingService imageProcessingService;

	public BackgroundRemovalService(ModelSessionManager modelSessionManager, ImageProcessingService imageProcessingService) {
		this.modelSessionManager = modelSessionManager;
		this.imageProcessingService = imageProcessingService;

		// this.automatePassportPhotoService = automatePassportPhotoService;
	}

	/**
     * Retrieves the active ONNX session.
     */
	public OrtSession getSession() {
		return modelSessionManager.getSession();
	}

	/**
     * Validates if an image has non-zero dimensions and is not null.
     */
	public boolean validateImg(BufferedImage image, int width, int height){
		return image != null && width > 0 && height > 0;
	}

	/**
     * Full image processing pipeline:
     * 1. Validate
     * 2. Preprocess to float[]
     * 3. Run model (ONNX)
     * 4. Postprocess with blending
     * 5. Encode to base64
     */
	public String processImage(MultipartFile file, String colorString, String backgroundString) throws OrtException, IOException {
		BufferedImage image = ImageIO.read(file.getInputStream());
		System.out.printf("%d %d Before",image.getHeight(),image.getWidth());
		String base64Str = imageProcessingService.encodeToBase64(image);
		String resizedImg = imageProcessingService.resizeToClosestMultipleOf32(base64Str);
		image = imageProcessingService.base64ToBufferedImage(resizedImg);
		System.out.println("HELLO");
		int rh = image.getHeight();
		int rw = image.getWidth();

		boolean validateImg = validateImg(image, rh, rw);
		if (!(validateImg)) {
			throw new ImageInvalidFormatException("Invalid image format" );
		}
		
		float[] imgData = preprocessImg(image);
		float[] outputArray = runModel(imgData, rh, rw);
		BufferedImage foreground = postprocessImg(colorString,backgroundString,outputArray,image,rw,rh);
		foreground = resizeImage(foreground, rw, rh);
		System.out.printf("%d %d Before",foreground.getHeight(),foreground.getWidth());
		String processedBase64 = encodeImageToBase64(foreground);
		return processedBase64;
	}

	/**
     * Prepares a BufferedImage for inference by converting to ARGB and extracting RGB float data.
     */
	public float[] preprocessImg(BufferedImage image){
		BufferedImage img = convertToARGB(image);
		float[] imgData = extractImageData(img);
		return imgData;
	}

	/**
     * Runs the ONNX model and returns a flat float[] output.
     */
	public float[] runModel(float[] imgData, int imageHeight, int imageWidth) throws OrtException {
		
		OrtSession session = modelSessionManager.getSession();
		OrtEnvironment env = OrtEnvironment.getEnvironment();
		OnnxTensor inputTensor = OnnxTensor.createTensor(
				env,
				FloatBuffer.wrap(imgData),
				new long[] { 1, 3, imageHeight, imageWidth }
		);
		
		OrtSession.Result result = session.run(Collections.singletonMap("input", inputTensor));
		float[] outputArray = ((OnnxTensor) result.get(0)).getFloatBuffer().array();
		
		inputTensor.close();
		return outputArray;
	}

	/**
     * Converts a flat ONNX model output into a 2D matte array (H x W) with alpha values.
     */
    private float[][] createMatte2D(float[] outputArray, int width, int height) {
        float[][] matte = new float[height][width];
        int index = 0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float val = outputArray[index++];
                matte[y][x] = val;
            }
        }
        return matte;
    }

	/**
     * Converts an image into float[] with channels-first format and RGB normalization.
     */
    private float[] extractImageData(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        float[] tensor = new float[3 * width * height];
        
		int idxR = 0;
        int idxG = width * height;
        int idxB = 2 * width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb) & 0xFF;

                tensor[idxR++] = r / 255.0f;
                tensor[idxG++] = g / 255.0f;
                tensor[idxB++] = b / 255.0f;
            }
        }

		return tensor;
    }

	/**
     * Applies background blending with either a solid color or a custom image background.
     */
	public BufferedImage postprocessImg(String colorString,String backgroundString, float[] outputArray,BufferedImage image,int rh,int rw){

		float[][] matte2D = createMatte2D(outputArray, image.getWidth(), image.getHeight());
		
		if (colorString == null) {
			colorString = Constants.DEFAULT_BACKGROUND_COLOR;  // Default value for colorString
		}
	
		System.out.printf("Here the background %s",backgroundString);
		BufferedImage foreground = alphaBlend(image, matte2D, backgroundString , colorString);

		return foreground;
	}

	private BufferedImage resizeImage(BufferedImage src, int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(src, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return resized;
    }


	/**
     * Performs alpha blending of the foreground image with a background (solid or custom).
     */
	public BufferedImage alphaBlend(BufferedImage original, float[][] matte, String background, String hexColor) {

		int width = original.getWidth();
		int height = original.getHeight();
	
		BufferedImage blended = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	
		BufferedImage resizedBackground;
		
		if (background != null) {
			BufferedImage customImage = decodeBase64ToImage(background);
			resizedBackground = resizeImageWithAspectRatio(customImage, width, height);
		} else {
			resizedBackground = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = resizedBackground.createGraphics();
			Color bgColor = Color.decode(hexColor);
			g2d.setColor(bgColor);
			g2d.fillRect(0, 0, width, height);
			g2d.dispose();
		}
	
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int originalPixel = original.getRGB(x, y);
				int backgroundPixel = resizedBackground.getRGB(x, y);
	
				float alpha = matte[y][x];
	
				// Extract foreground color components
				int oR = (originalPixel >> 16) & 0xFF;
				int oG = (originalPixel >> 8) & 0xFF;
				int oB = (originalPixel) & 0xFF;
	
				// Extract background color components
				int bR = (backgroundPixel >> 16) & 0xFF;
				int bG = (backgroundPixel >> 8) & 0xFF;
				int bB = (backgroundPixel) & 0xFF;
	
				// Blend colors
				int outR = (int) (oR * alpha + bR * (1 - alpha));
				int outG = (int) (oG * alpha + bG * (1 - alpha));
				int outB = (int) (oB * alpha + bB * (1 - alpha));
	
				// Set output pixel with full opacity (255)
				int outPixel = (255 << 24) | (outR << 16) | (outG << 8) | outB;
				blended.setRGB(x, y, outPixel);
			}
		}
	
		return blended;
	}

	/**
	 * Converts any BufferedImage to ARGB format if it isn't already.
	 */
	private BufferedImage convertToARGB(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_ARGB) {
            return image;
        }
        BufferedImage newImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2d = newImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return newImage;
    }

	/**
     * Encodes a BufferedImage into a base64-encoded PNG data URI.
	 * 
	 * @param image the image to encode
	 * @return a data URL string, or null if encoding fails
     */
    private String encodeImageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

	/**
	 *  Resizes the image while maintaining its aspect ratio, and centers it on a transparent canvas.
	 */
	private BufferedImage resizeImageWithAspectRatio(BufferedImage image, int targetWidth, int targetHeight) {
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
    
        double aspectRatio = (double) originalWidth / originalHeight;
        int newWidth = targetWidth;
        int newHeight = (int) (targetWidth / aspectRatio);
    
        if (targetHeight > targetWidth) {
            newHeight = targetHeight;
            newWidth = (int) (targetHeight * aspectRatio);
        } 

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
    
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    
        int x = (targetWidth - newWidth) / 2;
        int y = (targetHeight - newHeight) / 2;

        g2d.drawImage(image, x, y, newWidth, newHeight, null);
        g2d.dispose();
    
        return resizedImage;
    }

	/**
	 * Decodes a base64-encoded image (data URI format) into a BufferedImage.
	 * @param base64String a base64 string with data URI prefix
	 * @return the decoded BufferedImage, or null if decoding fails
	 */
	private BufferedImage decodeBase64ToImage(String base64String) {
        try {
            String[] parts = base64String.split(",");
            if (parts.length != 2) {
                return null;
            }
            byte[] imageBytes = Base64.getDecoder().decode(parts[1]);
            return ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}