package com.passportphoto.controller;

import ai.onnxruntime.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.FloatBuffer;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.awt.image.DataBufferByte;

@RestController
@RequestMapping("/image2")
@CrossOrigin(origins = "http://localhost:5173")
public class MODNetController2 {

    private static final String MODEL_NAME = "modnet.onnx";
    private final OrtSession session;

    private static final int MODEL_INPUT_WIDTH = 512;
    private static final int MODEL_INPUT_HEIGHT = 512;

    public MODNetController2() throws Exception {
        this.session = loadModel();
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, String>> processImage(@RequestBody Map<String, String> request) {
        try {
            // Decode Base64 image
            String base64Image = request.get("image");
            BufferedImage image = decodeBase64ToImage(base64Image);
            int rh = image.getHeight();
            int rw = image.getWidth();
            if (image == null) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Invalid image format"));
            }
            image = convertRe(image);

            String processedBase642 = encodeImageToBase64(image);
            return ResponseEntity.ok(Collections.singletonMap("processedImage", processedBase642));

            // Make sure we have a consistent type (ARGB)
            // image = convertToARGB(image);

            // BufferedImage resizedImage = resizeImage(image, MODEL_INPUT_WIDTH, MODEL_INPUT_HEIGHT);

            // Run ONNX inference
            // OrtEnvironment env = OrtEnvironment.getEnvironment();
            // // Extract the float[] input data (RGB in 0..1)
            // float[] inputData = extractImageData(image);
            // // Create OnnxTensor in shape [1,3,H,W]
            // OnnxTensor inputTensor = OnnxTensor.createTensor(
            //         env,
            //         FloatBuffer.wrap(inputData),
            //         new long[] { 1, 3, image.getHeight(), image.getWidth() });
            // OrtSession.Result result = session.run(Collections.singletonMap("input", inputTensor));

            // // Extract the ONNX output into float[]
            // float[] outputArray = ((OnnxTensor) result.get(0)).getFloatBuffer().array();

            // // Convert this to 2D matte (0..1)
            // float[][] matte2D = createMatte2D(outputArray, image.getWidth(), image.getHeight());
            // System.out.println(rh);
            // System.out.println(rw);
            // float[][] resizedMatte = resizeMatte(matte2D, rh, rw);

            // // Perform alpha blending with white background
            // BufferedImage foreground = alphaBlendWithWhite(image, matte2D);

            // // Encode to base64 and return
            // String processedBase64 = encodeImageToBase64(foreground);
            // return ResponseEntity.ok(Collections.singletonMap("processedImage", processedBase64));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Image processing failed"));
        }
    }
    public static BufferedImage ensure3Channels(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            // Convert grayscale to RGB
            BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            rgbImage.getGraphics().drawImage(image, 0, 0, null);
            System.out.println("GRAYY channel");
            return rgbImage;
        } else if (image.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
            // Remove alpha channel
            BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            rgbImage.getGraphics().drawImage(image, 0, 0, null);
            System.out.println("4BYTE ABGR channel");
            return rgbImage;
        }
        return image;
    }

    public static double[] getScaleFactor(int imH, int imW, int refSize) {
        double[] scaleFactors = new double[2];
        int imRh, imRw;

        if (Math.max(imH, imW) < refSize || Math.min(imH, imW) > refSize) {
            if (imW >= imH) {
                imRh = refSize;
                imRw = (int) (imW / (double) imH * refSize);
            } else {
                imRw = refSize;
                imRh = (int) (imH / (double) imW * refSize);
            }
        } else {
            imRh = imH;
            imRw = imW;
        }

        imRw = imRw - imRw % 32;
        imRh = imRh - imRh % 32;

        scaleFactors[0] = imRw / (double) imW; // x_scale_factor
        scaleFactors[1] = imRh / (double) imH; // y_scale_factor

        return scaleFactors;
    }

    // Normalize pixel values to the range [-1, 1]
    public static void normalizeImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = (byte) ((pixels[i] & 0xFF) - 127.5); // Subtract 127.5
            pixels[i] = (byte) (pixels[i] / 127.5);          // Divide by 127.5
        }
    }

    private BufferedImage convertRe(BufferedImage im) {
        im = ensure3Channels(im);
        int refSize = 512;
        // Normalize values to scale it between -1 to 1
        normalizeImage(im);

        // int imH = im.getHeight();
        // int imW = im.getWidth();
        // double[] scaleFactors = getScaleFactor(imH, imW, refSize);
        // double xScaleFactor = scaleFactors[0];
        // double yScaleFactor = scaleFactors[1];

        // // Resize image
        // int newWidth = (int) (imW * xScaleFactor);
        // int newHeight = (int) (imH * yScaleFactor);
        // BufferedImage resizedIm = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_3BYTE_BGR);
        // resizedIm.getGraphics()
        //         .drawImage(im.getScaledInstance(newWidth, newHeight, java.awt.Image.SCALE_AREA_AVERAGING), 0, 0, null);
        return im;
    }

    private BufferedImage resizeImage(BufferedImage image, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return resizedImage;
    }

    private float[][] resizeMatte(float[][] matte, int targetWidth, int targetHeight) {
        float[][] resizedMatte = new float[targetHeight][targetWidth];
        int originalHeight = matte.length;
        int originalWidth = matte[0].length;

        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                // Map the coordinates from the target dimensions to the original dimensions
                int srcX = x * originalWidth / targetWidth;
                int srcY = y * originalHeight / targetHeight;
                resizedMatte[y][x] = matte[srcY][srcX];
            }
        }
        return resizedMatte;
    }

    /**
     * Load the ONNX model from resources.
     */
    private OrtSession loadModel() throws Exception {
        InputStream modelStream = getClass().getClassLoader().getResourceAsStream(MODEL_NAME);
        if (modelStream == null) {
            throw new FileNotFoundException("MODNet model not found in resources: " + MODEL_NAME);
        }

        File tempModelFile = File.createTempFile("modnet", ".onnx");
        tempModelFile.deleteOnExit();

        try (OutputStream out = new FileOutputStream(tempModelFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = modelStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        OrtEnvironment env = OrtEnvironment.getEnvironment();
        return env.createSession(tempModelFile.getAbsolutePath(), new OrtSession.SessionOptions());
    }

    /**
     * Decodes a Base64 string (data URI) into a BufferedImage.
     */
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
     * Convert the input BufferedImage to ARGB type if not already.
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

    /**
     * Convert the flat float[] (model output) to a 2D [H][W] matte in [0..1].
     */
    private float[][] createMatte2D(float[] outputArray, int width, int height) {
        float[][] matte = new float[height][width];
        int index = 0;
        // The model typically outputs [1,1,H,W] or something similar, so we have H*W
        // floats
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

    /**
     * Blend the original image with a white background using the matte as alpha.
     * foreground = original * alpha + white * (1 - alpha)
     */
    private BufferedImage alphaBlendWithWhite(BufferedImage original, float[][] matte) {
        int width = original.getWidth();
        int height = original.getHeight();

        // Output: ARGB
        BufferedImage blended = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int rgb = original.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb) & 0xFF;

                float alpha = matte[y][x]; // in [0..1]
                float invAlpha = 1.0f - alpha;

                int outR = (int) (r * alpha + 255 * invAlpha);
                int outG = (int) (g * alpha + 255 * invAlpha);
                int outB = (int) (b * alpha + 255 * invAlpha);

                // Set alpha to 255 (fully opaque) in the output
                int outA = 255;

                int outPixel = (outA << 24) | (outR << 16) | (outG << 8) | outB;
                blended.setRGB(x, y, outPixel);
            }
        }
        return blended;
    }

    /**
     * Encode a BufferedImage to Base64 data URI (e.g.
     * "data:image/png;base64,....").
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
}