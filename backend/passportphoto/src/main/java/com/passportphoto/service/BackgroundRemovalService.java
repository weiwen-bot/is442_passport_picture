package com.passportphoto.service;

import com.passportphoto.repository.ImageRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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

@Service
public class BackgroundRemovalService {

	// private final ImageRepository imageRepository;
	// @Autowired
	// public BackgroundRemovalService(ImageRepository imageRepository){
	// // this.imageRepository = imageRepository;
	// }

	public byte[] extractFace(ImgDTO imgDTO, MultipartFile file) throws CvException {

		// System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		try {
			// your code that may throw IOException
			// For example:
			// FileInputStream file = new
			// FileInputStream(imgDTO.getFileNameWithCompletePath());
			byte[] bytes = file.getBytes();
			System.out.println(imgDTO.getHeight());
			System.out.println(imgDTO.getxOne());
			System.out.println(imgDTO.getyOne());

			// Point p1 = new Point(xOne,yOne);
			// Point p2 = new Point(xTwo,yTwo);

			Mat image = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_COLOR);
			// Mat image = Imgcodecs.imread(imgDTO.getFileNameWithCompletePath());

			Rect rectangle = new Rect(imgDTO.getxOne(), imgDTO.getyOne(), imgDTO.getWidth(), imgDTO.getHeight());
			// Rect rectangle = new Rect(p1,p2);
			Mat result = new Mat();
			Mat bgdModel = new Mat();
			Mat fgdModel = new Mat();
			Mat source = new Mat(1, 1, CvType.CV_8U, new Scalar(3));
			// Mat image = imgDTO.base64ToMat(imgDTO.getImageBase64());
			Imgproc.grabCut(image, result, rectangle, bgdModel, fgdModel, 8, Imgproc.GC_INIT_WITH_RECT);
			Core.compare(result, source, result, Core.CMP_EQ);
			Mat foreground = new Mat(image.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
			image.copyTo(foreground, result);

			MatOfByte matOfByte = new MatOfByte();
			Imgcodecs.imencode(".jpeg", foreground, matOfByte);
			return matOfByte.toArray();
			// Imgcodecs.imwrite("weiwenphoto-grayq1.jpg", foreground);
		} catch (IOException e) {
			// Handle the exception
			e.printStackTrace();
		}
		return null;
	}
}
