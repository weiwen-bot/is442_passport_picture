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

public class backgroundRemovalController {

    private final BackgroundRemovalService backgroundRemovalService;

    public backgroundRemovalController(BackgroundRemovalService backgroundRemovalService) {
        this.backgroundRemovalService = backgroundRemovalService;
    }

    // @PostMapping("/transform")
    // public ResponseEntity<byte[]> transformImg(@RequestParam("file") MultipartFile file, @ModelAttribute ImgDTO imgDTO) {
    //     try {
    //         byte[] result = backgroundRemoval.extractFace(imgDTO, file); // Transform logic
    //          HttpHeaders headers = new HttpHeaders();
    //         headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=processed.png");
    //         headers.add(HttpHeaders.CONTENT_TYPE, "image/png");
    //         return new ResponseEntity<>(result, headers, HttpStatus.OK); // Return transformed image
    //     } catch (Exception e) {
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    //     }
    // }

    @PostMapping("/removebg")
    public ResponseEntity<Map<String, String>> removalbg(@RequestBody Map<String, String> request) {
        try {
            String processedBase64 = backgroundRemovalService.processImage(request);
            return ResponseEntity.ok(Collections.singletonMap("processedImage", processedBase64));
        } catch (ImageInvalidFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Image processing failed"));
        }
    }

}
