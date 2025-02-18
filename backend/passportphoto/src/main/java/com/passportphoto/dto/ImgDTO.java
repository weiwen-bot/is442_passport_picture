package com.passportphoto.dto;

import java.util.Base64;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;

import lombok.Getter;
import lombok.Setter;


public class ImgDTO {
    // private String imageBase64;  // Store Mat as Base64 string
    private String fileNameWithCompletePath;
    private int xOne;
    private int yOne;
    private int width;
    private int height;

    

    // public String getImageBase64() {
    //     return imageBase64;
    // }

    // public void setImageBase64(String imageBase64) {
    //     this.imageBase64 = imageBase64;
    // }

    public String getFileNameWithCompletePath() {
        return fileNameWithCompletePath;
    }

    public void setFileNameWithCompletePath(String fileNameWithCompletePath) {
        this.fileNameWithCompletePath = fileNameWithCompletePath;
    }

    public int getxOne() {
        return xOne;
    }

    public void setxOne(int xOne) {
        this.xOne = xOne;
    }

    public int getyOne() {
        return yOne;
    }

    public void setyOne(int yOne) {
        this.yOne = yOne;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String matToBase64(Mat image) {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".png", image, matOfByte);
        byte[] byteArray = matOfByte.toArray();
        return Base64.getEncoder().encodeToString(byteArray);
    }

    public Mat base64ToMat(String base64String) {
        byte[] decodedBytes = Base64.getDecoder().decode(base64String);
        return Imgcodecs.imdecode(new MatOfByte(decodedBytes), Imgcodecs.IMREAD_UNCHANGED);
    }
    
}

