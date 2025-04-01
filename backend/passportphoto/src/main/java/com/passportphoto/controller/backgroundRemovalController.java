package com.passportphoto.controller;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.passportphoto.dto.ImgDTO;

import com.passportphoto.service.BackgroundRemovalService;
import org.springframework.web.multipart.MultipartFile;

import com.passportphoto.exceptions.*;

@RestController
@RequestMapping("/bg")
@CrossOrigin(origins = "http://localhost:5173")

public class BackgroundRemovalController {

    private final BackgroundRemovalService backgroundRemovalService;

    public BackgroundRemovalController(BackgroundRemovalService backgroundRemovalService) {
        this.backgroundRemovalService = backgroundRemovalService;
    }

    @PostMapping("/removebg")
    public ResponseEntity<Map<String, String>> removalbg(
            @RequestParam(value = "image", required = false) MultipartFile file,
            @RequestParam(value = "colorString", required = false) String colorString,
            @RequestParam(value = "backgroundString", required = false) String backgroundString) {
        try {
            String processedBase64 = backgroundRemovalService.processImage(file, colorString, backgroundString);
            return ResponseEntity.ok(Collections.singletonMap("processedImage", processedBase64));
        } catch (ImageInvalidFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Image processing failed"));
        }
    }

}
