package background;

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

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvException;

/**
 * A simple class that demonstrates/tests the usage of the OpenCV library in
 * Java. It prints a 3x3 identity matrix and then converts a given image in gray
 * scale.
 * 
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @since 2013-10-20
 * 
 */
public class HelloCV {
	private static Mat doCanny(Mat frame) {
		// init
		Mat grayImage = new Mat();
		Mat detectedEdges = new Mat();

		// convert to grayscale
		Imgproc.cvtColor(frame, grayImage, Imgproc.COLOR_BGR2GRAY);

		// reduce noise with a 3x3 kernel
		Imgproc.blur(grayImage, detectedEdges, new Size(3, 3));

		double threshold1 = 1.0;
		double threshold2 = 3.0;

		// canny detector, with ratio of lower:upper threshold of 3:1
		Imgproc.Canny(detectedEdges, detectedEdges, threshold1, threshold2);

		// using Canny's output as a mask, display the result
		Mat dest = new Mat();
		frame.copyTo(dest, detectedEdges);

		return dest;
	}

	private static double getHistAverage(Mat hsvImg, Mat hueValues) {
		// init
		double average = 0.0;
		Mat hist_hue = new Mat();
		// 0-180: range of Hue values
		MatOfInt histSize = new MatOfInt(180);
		List<Mat> hue = new ArrayList<>();
		hue.add(hueValues);

		// compute the histogram
		Imgproc.calcHist(hue, new MatOfInt(0), new Mat(), hist_hue, histSize, new MatOfFloat(0, 179));

		// get the average Hue value of the image
		// (sum(bin(h)*h))/(image-height*image-width)
		// -----------------
		// equivalent to get the hue of each pixel in the image, add them, and
		// divide for the image size (height and width)
		for (int h = 0; h < 180; h++) {
			// for each bin, get its value and multiply it for the corresponding
			// hue
			average += (hist_hue.get(h, 0)[0] * h);
		}

		// return the average hue of the image
		return average = average / hsvImg.size().height / hsvImg.size().width;
	}

	private static Mat doBackgroundRemoval(Mat frame) {
		// init
		double threshold = 1.0;
		Mat hsvImg = new Mat();
		List<Mat> hsvPlanes = new ArrayList<>();
		Mat thresholdImg = new Mat();

		// int thresh_type = Imgproc.THRESH_BINARY_INV;
		// int thresh_type = Imgproc.THRESH_TOZERO;
		int thresh_type = Imgproc.THRESH_BINARY;
		// if (this.inverse.isSelected())
		// thresh_type = Imgproc.THRESH_BINARY;

		// threshold the image with the average hue value
		hsvImg.create(frame.size(), CvType.CV_8U);
		Imgproc.cvtColor(frame, hsvImg, Imgproc.COLOR_BGR2HSV);
		Core.split(hsvImg, hsvPlanes);

		// get the average hue value of the image
		double threshValue = getHistAverage(hsvImg, hsvPlanes.get(0));

		Imgproc.threshold(hsvPlanes.get(0), thresholdImg, threshValue, 179.0, thresh_type);

		Imgproc.blur(thresholdImg, thresholdImg, new Size(5, 5));

		// dilate to fill gaps, erode to smooth edges
		Imgproc.dilate(thresholdImg, thresholdImg, new Mat(), new Point(-1, -1), 1);
		Imgproc.erode(thresholdImg, thresholdImg, new Mat(), new Point(-1, -1), 3);

		Imgproc.threshold(thresholdImg, thresholdImg, threshValue, 179.0, Imgproc.THRESH_BINARY);

		// create the new image
		Mat foreground = new Mat(frame.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
		frame.copyTo(foreground, thresholdImg);

		return foreground;
	}

	private static Mat doBackgroundRemovalFloodFill(Mat frame) {

		Scalar newVal = new Scalar(255, 255, 255);
		Scalar loDiff = new Scalar(50, 50, 50);
		Scalar upDiff = new Scalar(50, 50, 50);
		Point seedPoint = new Point(50, 50);
		Mat mask = new Mat();
		Rect rect = new Rect();

		// Imgproc.floodFill(frame, mask, seedPoint, newVal);
		Imgproc.floodFill(frame, mask, seedPoint, newVal, rect, loDiff, upDiff, Imgproc.FLOODFILL_FIXED_RANGE);

		return frame;
	}

	public static void extractFace(Mat image, String fileNameWithCompletePath,
			int xOne, int yOne, int width, int height) throws CvException {

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		// Point p1 = new Point(xOne,yOne);
		// Point p2 = new Point(xTwo,yTwo);

		Rect rectangle = new Rect(xOne, yOne, width, height);
		// Rect rectangle = new Rect(p1,p2);
		Mat result = new Mat();
		Mat bgdModel = new Mat();
		Mat fgdModel = new Mat();
		Mat source = new Mat(1, 1, CvType.CV_8U, new Scalar(3));
		Imgproc.grabCut(image, result, rectangle, bgdModel, fgdModel, 8, Imgproc.GC_INIT_WITH_RECT);
		Core.compare(result, source, result, Core.CMP_EQ);
		Mat foreground = new Mat(image.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
		image.copyTo(foreground, result);
		Imgcodecs.imwrite("weiwenphoto-grayq1.jpg", foreground);
	}
	public static void resize(Mat image){
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		int originalWidth = image.cols();
        int originalHeight = image.rows();

		double aspectRatio = (double) originalWidth / originalHeight;

		int targetWidth = 400;
		int targetHeight = 514;

		int newWidth = targetWidth;
        int newHeight = (int) (targetWidth / aspectRatio);

        if (newHeight > targetHeight) {
            newHeight = targetHeight;
            newWidth = (int) (targetHeight * aspectRatio);
        }
		newHeight = targetHeight;
		Mat destination = new Mat();
		Size size = new Size(newWidth, newHeight);
		Imgproc.resize(image, destination, size);

		Mat outputImage = new Mat(targetHeight, targetWidth, image.type(), new Scalar(255, 255, 255));

        // Compute top-left corner for centering
        int xOffset = (targetWidth - newWidth) / 2;
        int yOffset = (targetHeight - newHeight) / 2;

        // Place the resized image onto the center of the blank image
        Rect roi = new Rect(xOffset, yOffset, newWidth, newHeight);
        destination.copyTo(outputImage.submat(roi));

		System.out.println(destination.rows() + " " + destination.cols());
		Imgcodecs.imwrite("weiwenphoto-resized.jpg", destination);
		

	}

	public static void main(String[] args) {
		// load the OpenCV native library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		// create and print on screen a 3x3 identity matrix
		// System.out.println("Create a 3x3 identity matrix...");
		// Mat mat = Mat.eye(3, 3, CvType.CV_8UC1);
		// System.out.println("mat = " + mat.dump());

		// prepare to convert a RGB image in gray scale
		String location = "weiwenphoto1.jpg";
		System.out.print("Convert the image at " + location + " in gray scale... ");
		// get the jpeg image from the internal resource folder
		Mat image = Imgcodecs.imread(location);

		int height = image.rows();
        int width = image.cols();

        // Print the height and width
        System.out.println("Image Height: " + height);
        System.out.println("Image Width: " + width);
		// convert the image in gray scale
		// Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);

		// Mat newImage = doBackgroundRemovalFloodFill(image);

		extractFace(image,location,100,200,width,height);
		// resize(image);

		// write the new image on disk
		// Imgcodecs.imwrite("weiwenphoto-gray.jpg", newImage);
		System.out.println("Done!");
	}
}