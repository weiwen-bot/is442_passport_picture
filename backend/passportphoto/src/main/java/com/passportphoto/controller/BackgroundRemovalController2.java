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

@RestController
@RequestMapping("/image2")
@CrossOrigin(origins = "http://localhost:5173")
public class BackgroundRemovalController2 {

    private static final int MODEL_SIZE = 1024; // match demo_onnx.py
    private final OrtSession session;

    public BackgroundRemovalController2() throws Exception {
        this.session = loadModel();
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, String>> removeBackground(@RequestBody Map<String, String> request) {
        try {
            // 1) Decode the Base64 image
            String base64Image = request.get("image");
            BufferedImage originalImage = decodeBase64ToImage(base64Image);
            if (originalImage == null) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Invalid image format"));
            }
            int origW = originalImage.getWidth();
            int origH = originalImage.getHeight();

            // 2) Resize to 1024x1024 for inference
            BufferedImage resizedInput = resizeImage(
                originalImage, MODEL_SIZE, MODEL_SIZE,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );

            // 3) Convert to float[] in [-0.5..0.5]
            float[] inputData = extractImageDataForISNet(resizedInput);
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OnnxTensor inputTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(inputData),
                new long[]{1, 3, MODEL_SIZE, MODEL_SIZE}
            );

            // 4) Run model
            String inputName = session.getInputNames().iterator().next();
            OrtSession.Result result = session.run(Collections.singletonMap(inputName, inputTensor));

            // 5) Model output => [0..1], then scale to [0..255]
            float[] outputArray = ((OnnxTensor) result.get(0)).getFloatBuffer().array();
            float[][] mask2D_1024 = createMask2D(outputArray, MODEL_SIZE, MODEL_SIZE);

            // 6) Convert mask to a 1024x1024 grayscale BufferedImage
            BufferedImage maskImage1024 = createMaskImage(mask2D_1024); 

            // 7) Resize the mask back to original size
            //    (demo_onnx.py does output = cv2.resize(output, (origW, origH), INTER_LANCZOS4))
            BufferedImage maskImageOriginalSize = resizeImage(
                maskImage1024, origW, origH,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );

            // 8) Apply the mask as alpha on the ORIGINAL image => remove background
            //    Now you get an ARGB image where alpha = mask, background is transparent.
            BufferedImage transparentImage = applyMaskAsAlpha(originalImage, maskImageOriginalSize);

            // 9) Encode final ARGB image (with transparency) as PNG Base64
            String base64Result = encodeImageToBase64(transparentImage);

            // Return the final “processedImage” to match your front end
            return ResponseEntity.ok(Collections.singletonMap("processedImage", base64Result));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Inference failed"));
        }
    }

    // ------------------------------------------------------------------------------------------
    // Helper Methods
    // ------------------------------------------------------------------------------------------

    /**
     * Load isnet.onnx
     */
    private OrtSession loadModel() throws Exception {
        InputStream modelStream = getClass().getClassLoader().getResourceAsStream("isnet.onnx");
        if (modelStream == null) {
            throw new FileNotFoundException("isnet.onnx not found in resources folder.");
        }
        File tempModelFile = File.createTempFile("isnet", ".onnx");
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
     * Decodes a Base64 string into a BufferedImage, e.g. "data:image/png;base64,iVBOR..."
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

    /**
     * Resize an image to targetWidth x targetHeight using bilinear or bicubic interpolation
     */
    private BufferedImage resizeImage(BufferedImage src, int targetWidth, int targetHeight, Object interpolationHint) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolationHint);
        g2d.drawImage(src, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return resized;
    }

    /**
     * Convert 1024x1024 image -> float array [3,H,W] in [-0.5..0.5]
     */
    private float[] extractImageDataForISNet(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        float[] data = new float[3 * width * height];

        int idxR = 0;
        int idxG = width * height;
        int idxB = 2 * width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);

                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8)  & 0xFF;
                int b = (argb)       & 0xFF;

                // [0..1], then shift to [-0.5..0.5]
                float rf = (r / 255.0f) - 0.5f;
                float gf = (g / 255.0f) - 0.5f;
                float bf = (b / 255.0f) - 0.5f;

                data[idxR++] = rf;
                data[idxG++] = gf;
                data[idxB++] = bf;
            }
        }
        return data;
    }

    /**
     * Convert raw model output [0..1] to [0..255] in a 2D array
     */
    private float[][] createMask2D(float[] outputArray, int width, int height) {
        float[][] mask = new float[height][width];
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float val = outputArray[index++];
                mask[y][x] = val * 255.0f;
            }
        }
        return mask;
    }

    /**
     * Convert 2D float array (0..255) -> grayscale BufferedImage
     */
    private BufferedImage createMaskImage(float[][] mask2D) {
        int h = mask2D.length;
        int w = mask2D[0].length;
        BufferedImage maskImage = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int gray = Math.round(mask2D[y][x]);
                // ARGB => 0xFF alpha, then gray in R/G/B
                int rgb = (0xFF << 24) | (gray << 16) | (gray << 8) | gray;
                maskImage.setRGB(x, y, rgb);
            }
        }
        return maskImage;
    }

    /**
     * Combine the original image with the mask as alpha:
     *   - alpha = mask[y][x]  (0..255)
     *   - color = originalImage's R,G,B
     *   - background => transparent
     */
    private BufferedImage applyMaskAsAlpha(BufferedImage original, BufferedImage maskImage) {
        int w = original.getWidth();
        int h = original.getHeight();
        // ARGB image with alpha
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // Original color
                int origARGB = original.getRGB(x, y);
                int r = (origARGB >> 16) & 0xFF;
                int g = (origARGB >> 8)  & 0xFF;
                int b = (origARGB)       & 0xFF;

                // Mask grayscale => alpha channel
                int maskARGB = maskImage.getRGB(x, y);
                int maskGray = maskARGB & 0xFF; // same in R, G, B
                int alpha = maskGray;          // 0..255

                // Combine into ARGB
                int outPixel = (alpha << 24) | (r << 16) | (g << 8) | b;
                output.setRGB(x, y, outPixel);
            }
        }
        return output;
    }

    /**
     * Encode the final RGBA image (with transparency) as PNG in Base64
     */
    private String encodeImageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // PNG supports alpha transparency
        ImageIO.write(image, "png", baos);
        String encoded = Base64.getEncoder().encodeToString(baos.toByteArray());
        return "data:image/png;base64," + encoded;
    }
}
