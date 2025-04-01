package com.passportphoto.service;

import com.passportphoto.repository.ImageRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import com.passportphoto.dto.ImgDTO;

import com.passportphoto.service.ModelSessionManager;
import com.passportphoto.util.Constants;
import ai.onnxruntime.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import com.passportphoto.exceptions.*;

@Service
public class BackgroundRemovalService {

	private final ModelSessionManager modelSessionManager;

	// @Autowired
	// public BackgroundRemovalService(ImageRepository imageRepository){
	// this.modelSessionManager = imageRepository;
	// }

	public BackgroundRemovalService(ModelSessionManager modelSessionManager) {
		this.modelSessionManager = modelSessionManager;
	}

	public OrtSession getSession() {
		return modelSessionManager.getSession();
	}

	public float[] runModel(float[] imgData, int imageHeight, int imageWidth) throws OrtException {
		
		OrtSession session = modelSessionManager.getSession();
		OrtEnvironment env = OrtEnvironment.getEnvironment();
		OnnxTensor inputTensor = OnnxTensor.createTensor(
				env,
				FloatBuffer.wrap(imgData),
				new long[] { 1, 3, imageHeight, imageWidth } // Shape: (Batch, Channels, Height, Width)
		);
		OrtSession.Result result = session.run(Collections.singletonMap("input", inputTensor));

		// Extract the ONNX output into float[]
		float[] outputArray = ((OnnxTensor) result.get(0)).getFloatBuffer().array();
		// Close Resource
		inputTensor.close();
		// for (OnnxTensor tensor : result) {
        //     tensor.close();
        // }
		return outputArray;
	}
	public boolean validateImg(BufferedImage image, int width, int height){
		return image != null && width > 0 && height > 0;
	}

	/**
     * Extract float pixel data (normalized 0..1) in [C,H,W] order (channels-first).
     * Channels: R, G, B
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

	public float[] preprocessImg(BufferedImage image){
		// Make sure we have a consistent type (ARGB)
		BufferedImage img = convertToARGB(image);
		// Extract the float[] input data (RGB in 0..1)
		float[] imgData = extractImageData(img);

		return imgData;

	}


	public BufferedImage postprocessImg(String colorString,String backgroundString, float[] outputArray,BufferedImage image,int rh,int rw){
		// Convert this to 2D matte (0..1)
		float[][] matte2D = createMatte2D(outputArray, image.getWidth(), image.getHeight());
		
		if (colorString == null) {
			colorString = Constants.DEFAULT_BACKGROUND_COLOR;  // Default value for colorString
		}
	
		// Check if backgroundString is null and assign default value

		// String backgroundString = request.getOrDefault("backgroundString", null);
		System.out.printf("Here the background %s",backgroundString);
		// String colorString = request.getOrDefault("colorString", Constants.DEFAULT_BACKGROUND_COLOR);

		BufferedImage foreground = alphaBlend(image, matte2D, backgroundString , colorString);

		return foreground;
	}


	public BufferedImage alphaBlend(BufferedImage original, float[][] matte, String background, String hexColor) {
		int width = original.getWidth();
		int height = original.getHeight();
	
		BufferedImage blended = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	
		BufferedImage resizedBackground;
		
		if (background != null) {
			// Resize background to match the original image
			BufferedImage customImage = decodeBase64ToImage(background);
			resizedBackground = resizeImageWithAspectRatio(customImage, width, height);
		} else {
			// Create a solid color background
			resizedBackground = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = resizedBackground.createGraphics();
			Color bgColor = Color.decode(hexColor);
			g2d.setColor(bgColor);
			g2d.fillRect(0, 0, width, height);
			g2d.dispose();
		}
	
		// Perform blending
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int originalPixel = original.getRGB(x, y);
				int backgroundPixel = resizedBackground.getRGB(x, y);
	
				float alpha = matte[y][x]; // Alpha values between 0 (transparent) and 1 (opaque)
	
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
     * Encode a BufferedImage to Base64 data URI (e.g. "data:image/png;base64,....").
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

	private BufferedImage resizeImageWithAspectRatio(BufferedImage image, int targetWidth, int targetHeight) {
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
    
        // Calculate the new dimensions while maintaining the aspect ratio
        double aspectRatio = (double) originalWidth / originalHeight;
        int newWidth = targetWidth;
        int newHeight = (int) (targetWidth / aspectRatio);
    
        if (targetHeight > targetWidth) {
            newHeight = targetHeight;
            newWidth = (int) (targetHeight * aspectRatio);
        } 

        // Create a transparent image with the target dimensions
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
    
        // Enable high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    
        // Center the resized image within the target dimensions
        int x = (targetWidth - newWidth) / 2;
        int y = (targetHeight - newHeight) / 2;

        g2d.drawImage(image, x, y, newWidth, newHeight, null);
        g2d.dispose();
    
        return resizedImage;
    }
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


	private BufferedImage decodeBase64ToImage(String base64String) {
        try {
            // Usually the format is "data:image/png;base64,iVBORw0KGgo..."
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

	/**
     * Convert the flat float[] (model output) to a 2D [H][W] matte in [0..1].
     */
    private float[][] createMatte2D(float[] outputArray, int width, int height) {
        float[][] matte = new float[height][width];
        int index = 0;
        // The model typically outputs [1,1,H,W] or something similar, so we have H*W floats
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float val = outputArray[index++];
                // If your model outputs 0..1, no scaling needed.
                // If it's 0..255, then do: val /= 255.0f;
                matte[y][x] = val;
            }
        }
        return matte;
    }

	public String processImage(MultipartFile file, String colorString, String backgroundString) throws OrtException, IOException {
			// Decode Base64 image
		// String base64Image = request.get("image");
		// String category = request.get("category");
		// BufferedImage image = decodeBase64ToImage(base64Image);
		BufferedImage image = ImageIO.read(file.getInputStream());
		
		int rh = image.getHeight();
		int rw = image.getWidth();
		System.out.println();
		System.out.println("BG REMOVAL");
		System.out.println(rh);
		System.out.println(rw);

		boolean validateImg = validateImg(image, rh, rw);
		if (!(validateImg)) {
			throw new ImageInvalidFormatException("Invalid image format" );
		}
		
		float[] imgData = preprocessImg(image);

		// Create OnnxTensor in shape [1,3,H,W]
		float[] outputArray = runModel(imgData, rh, rw);

		BufferedImage foreground = postprocessImg(colorString,backgroundString,outputArray,image,rw,rh);

		// Encode to base64 and return
		String processedBase64 = encodeImageToBase64(foreground);
		return processedBase64;
	}

}
