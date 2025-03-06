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

@RestController
@RequestMapping("/image3")
@CrossOrigin(origins = "http://localhost:5173")
public class MODNetController3 {

    private static final String MODEL_NAME = "modnet.onnx";
    private final OrtSession session;

    public MODNetController3() throws Exception {
        this.session = loadModel();
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, String>> processImage(@RequestBody Map<String, String> request) {
        final int ref_size = 512;
        try {
            // Decode Base64 image
            String base64Image = request.get("image");

            Mat im = base64ToMat(base64Image);
            if (im.empty()) {
                System.out.println("Failed to decode image!");
                return null;
            }

            // Convert BGR to RGB
            Imgproc.cvtColor(im, im, Imgproc.COLOR_BGR2RGB);

            // Unify image channels to 3
            im = unifyChannels(im);

            // Normalize values to scale it between -1 to 1
            Mat imFloat = new Mat();
            im.convertTo(imFloat, CvType.CV_32F, 1.0 / 127.5, -1.0);

            // Get image dimensions
            int imH = imFloat.rows();
            int imW = imFloat.cols();
            int imC = imFloat.channels();

            // Calculate scaling factors
            double[] scaleFactors = getScaleFactor(imH, imW, ref_size);
            double xScaleFactor = scaleFactors[0];
            double yScaleFactor = scaleFactors[1];

            // Resize image
            Mat imResized = new Mat();
            Imgproc.resize(imFloat, imResized, new Size(), xScaleFactor, yScaleFactor, Imgproc.INTER_AREA);

            // Prepare input shape (transpose, swap axes, and expand dimensions)
            Mat imTransposed = new Mat();
            Core.transpose(imResized, imTransposed);
            Mat imSwappedAxes = swapAxes(imTransposed, 1, 2);
            Mat imExpanded = expandDims(imSwappedAxes, 0);

            // Convert Mat to float array
            float[] inputArray = matToFloatArray(imExpanded);

            // Initialize ONNX Runtime session

            OrtEnvironment env = OrtEnvironment.getEnvironment();
            // Extract the float[] input data (RGB in 0..1)
            float[] inputData = extractImageData(im);
            // Create OnnxTensor in shape [1,3,H,W]
            OnnxTensor inputTensor = OnnxTensor.createTensor(
                    env,
                    FloatBuffer.wrap(inputData),
                    new long[] { 1, 3, im.rows(), im.cols() });
            OrtSession.Result result = session.run(Collections.singletonMap("input",
                    inputTensor));

            // Get output tensor
            float[][][][] outputArray = (float[][][][]) result.get(0).getValue();

            // The output shape is typically [1, 1, H, W] for segmentation/matting models
            int height = outputArray[0][0].length; // Height of the matte
            int width = outputArray[0][0][0].length; // Width of the matte

            // Convert the 4D output to a 2D matte (0..1)
            float[][] matte2D = new float[height][width];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    matte2D[y][x] = outputArray[0][0][y][x]; // Extract the matte values
                }
            }

            // Refine matte
            Mat matte = refineMatte(matte2D, imW, imH);

            // Encode to base64 and return
            String processedBase64 = encodeImageToBase64(matte);
            System.out.println(processedBase64);
            return ResponseEntity.ok(Collections.singletonMap("processedImage", processedBase64));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Image processing failed"));
        }
    }

    public static String encodeImageToBase64(Mat mat) {
        // Convert Mat to byte array (PNG format)
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".png", mat, matOfByte);
        byte[] imageBytes = matOfByte.toArray();

        // Encode byte array to Base64
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // Return Base64 string with data URL scheme
        return "data:image/png;base64," + base64Image;
    }

    public static float[] extractImageData(Mat mat) {
        // Ensure the Mat is in 3-channel (RGB) format
        if (mat.channels() != 3) {
            throw new IllegalArgumentException("Input Mat must have 3 channels (RGB).");
        }

        int width = mat.cols();
        int height = mat.rows();
        float[] tensor = new float[3 * width * height];

        // Indices for R, G, B channels in the tensor
        int idxR = 0;
        int idxG = width * height;
        int idxB = 2 * width * height;

        // Iterate over the Mat and extract pixel data
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Get the pixel value at (x, y)
                double[] pixel = mat.get(y, x);

                // Extract R, G, B values (OpenCV stores pixels in BGR order)
                float b = (float) pixel[0] / 255.0f;
                float g = (float) pixel[1] / 255.0f;
                float r = (float) pixel[2] / 255.0f;

                // Store normalized values in the tensor
                tensor[idxR++] = r;
                tensor[idxG++] = g;
                tensor[idxB++] = b;
            }
        }

        return tensor;
    }

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

    public static Mat base64ToMat(String base64Image) {
        // Decode Base64 string to byte array
        // byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        String base64Data = base64Image.split(",")[1];

        // Decode the Base64 string
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            System.out.println("Image decoded successfully. Byte array length: " + imageBytes.length);
            return Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_UNCHANGED);
        } catch (IllegalArgumentException e) {
            System.err.println("Failed to decode Base64 string: " + e.getMessage());
        }
        return null;

        // Convert byte array to Mat

    }

    // Swap axes of a Mat (equivalent to np.swapaxes)
    public static Mat unifyChannels(Mat im) {
        int channels = im.channels();

        if (channels == 1) {
            // Convert grayscale to RGB
            Mat rgbImage = new Mat();
            Imgproc.cvtColor(im, rgbImage, Imgproc.COLOR_GRAY2RGB);
            return rgbImage;
        } else if (channels == 4) {
            // Convert RGBA to RGB
            Mat rgbImage = new Mat();
            Imgproc.cvtColor(im, rgbImage, Imgproc.COLOR_RGBA2RGB);
            return rgbImage;
        } else if (channels == 3) {
            // Already RGB, return as is
            return im;
        } else {
            throw new IllegalArgumentException("Unsupported number of channels: " + channels);
        }
    }

    // Calculate scaling factors
    public static double[] getScaleFactor(int imH, int imW, int refSize) {
        int imRh, imRw;

        if (Math.max(imH, imW) < refSize || Math.min(imH, imW) > refSize) {
            if (imW >= imH) {
                imRh = refSize;
                imRw = (int) ((double) imW / imH * refSize);
            } else {
                imRw = refSize;
                imRh = (int) ((double) imH / imW * refSize);
            }
        } else {
            imRh = imH;
            imRw = imW;
        }

        imRw = imRw - (imRw % 32);
        imRh = imRh - (imRh % 32);

        double xScaleFactor = (double) imRw / imW;
        double yScaleFactor = (double) imRh / imH;

        return new double[] { xScaleFactor, yScaleFactor };
    }

    // Swap axes of a Mat
    public static Mat swapAxes(Mat mat, int axis1, int axis2) {
        Mat swapped = new Mat(mat.cols(), mat.rows(), mat.type());
        for (int i = 0; i < mat.rows(); i++) {
            for (int j = 0; j < mat.cols(); j++) {
                swapped.put(j, i, mat.get(i, j));
            }
        }
        return swapped;
    }

    // Expand dimensions of a Mat
    public static Mat expandDims(Mat mat, int axis) {
        Mat expanded = new Mat(1, mat.rows() * mat.cols(), mat.type());
        for (int i = 0; i < mat.rows(); i++) {
            for (int j = 0; j < mat.cols(); j++) {
                expanded.put(0, i * mat.cols() + j, mat.get(i, j));
            }
        }
        return expanded;
    }

    // Convert Mat to float array
    public static float[] matToFloatArray(Mat mat) {
        float[] array = new float[(int) mat.total() * mat.channels()];
        mat.get(0, 0, array);
        return array;
    }

    // Refine matte and resize to original dimensions
    public static Mat refineMatte(float[][] output, int imW, int imH) {
        Mat matte = new Mat(output.length, output[0].length, CvType.CV_32F);
        for (int i = 0; i < output.length; i++) {
            matte.put(i, 0, output[i]);
        }

        // Convert to 8-bit and scale to [0, 255]
        Mat matte8u = new Mat();
        matte.convertTo(matte8u, CvType.CV_8U, 255);

        // Resize to original dimensions
        Mat matteResized = new Mat();
        Imgproc.resize(matte8u, matteResized, new Size(imW, imH), 0, 0, Imgproc.INTER_AREA);

        return matteResized;
    }
}