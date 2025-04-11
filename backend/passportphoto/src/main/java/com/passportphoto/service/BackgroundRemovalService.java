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
import org.opencv.core.Size;

import com.passportphoto.exceptions.FailedProcessingException;
import com.passportphoto.exceptions.ImageInvalidFormatException;
import com.passportphoto.exceptions.ImageInputException;
import com.passportphoto.util.Constants;
import com.passportphoto.util.ValidationUtil;
import com.passportphoto.util.ImageConverterUtil;
import com.passportphoto.util.ResizeUtil;

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
	private final Constants constants;


	public BackgroundRemovalService(ModelSessionManager modelSessionManager, Constants constants) {
		this.constants = constants;
		this.modelSessionManager = modelSessionManager;
		


	}

	/**
	 * Retrieves the active ONNX session.
	 */
	public OrtSession getSession() {
		return modelSessionManager.getSession();
	}
	/**
	 * Full image processing pipeline:
	 * 1. Validate
	 * 2. Preprocess to float[]
	 * 3. Run model (ONNX)
	 * 4. Postprocess with blending
	 * 5. Encode to base64
	 */
	public String processImage(MultipartFile file, String colorString, String backgroundString) throws Exception {

		try {
			BufferedImage image = ImageIO.read(file.getInputStream());
			int oh = image.getHeight();
			int ow = image.getWidth(); 

			image = ResizeUtil.resizeToNearestMultiple(image, constants.getModelSizeMultiplier());

			int rh = image.getHeight();
			int rw = image.getWidth();
			ValidationUtil.validateBufferedImage(image, rw, rh);

			float[] imgData = preprocessImg(image);

			float[] outputArray = runModel(imgData, rh, rw);

			BufferedImage foreground = postprocessImg(colorString, backgroundString, outputArray, image, rw, rh);

			foreground = ResizeUtil.resizeImage(foreground, ow, oh);
			
			String format = file.getContentType().split("/")[1];
			int width = foreground.getWidth();
			int height = foreground.getHeight();
			String processedBase64 = ImageConverterUtil.convertBufferedImgToBase64(foreground,"jpg");
			return processedBase64;

		} catch (OrtException e) {
			throw new FailedProcessingException("Image background removal failed", e);
		} catch (IOException e) {
			throw new ImageInputException("Failed to read input image", e);
		}

	}

	/**
	 * Prepares a BufferedImage for inference by converting to ARGB and extracting
	 * RGB float data.
	 */
	public float[] preprocessImg(BufferedImage image) {
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
				new long[] { 1, 3, imageHeight, imageWidth });

		OrtSession.Result result = session.run(Collections.singletonMap("input", inputTensor));
		float[] outputArray = ((OnnxTensor) result.get(0)).getFloatBuffer().array();

		inputTensor.close();
		return outputArray;
	}

	/**
	 * Converts a flat ONNX model output into a 2D matte array (H x W) with alpha
	 * values.
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
	 * Converts an image into float[] with channels-first format and RGB
	 * normalization.
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
	 * Applies background blending with either a solid color or a custom image
	 * background.
	 */
	public BufferedImage postprocessImg(String colorString, String backgroundString, float[] outputArray,
			BufferedImage image, int rh, int rw) throws Exception {

		float[][] matte2D = createMatte2D(outputArray, image.getWidth(), image.getHeight());

		if (colorString == null) {
			colorString = constants.getBackgroundColor(); // Default value for colorString
		}

		BufferedImage foreground = alphaBlend(image, matte2D, backgroundString, colorString);

		return foreground;
	}


	/**
	 * Performs alpha blending of the foreground image with a background (solid or
	 * custom).
	 */
	public BufferedImage alphaBlend(BufferedImage original, float[][] matte, String background, String hexColor)
			throws Exception {

		int width = original.getWidth();
		int height = original.getHeight();

		BufferedImage blended = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		BufferedImage resizedBackground;

		if (background != null) {
			BufferedImage customImage = ImageConverterUtil.base64ToBufferedImage(background);

			resizedBackground = ResizeUtil.resizeImageWithAspectRatio(customImage, width, height);
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
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = newImage.createGraphics();
		g2d.drawImage(image, 0, 0, null);
		g2d.dispose();
		return newImage;
	}




}