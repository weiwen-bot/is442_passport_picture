package com.passportphoto;

import org.opencv.core.Core;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;


@SpringBootApplication
public class PhotoprocessorApplication {
    static { System.load("C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.5.11-hotspot\\bin\\opencv_java4100.dll"); }

    public static void main(String[] args) {
        SpringApplication.run(PhotoprocessorApplication.class, args);
    }
}