/*
 * AutomatePassportPhotoController.java
 *
 * This controller handles the automated passport photo generation endpoint
 * for the Passport Picture Project.
 *
 */

package com.passportphoto.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.passportphoto.dto.AutomatedImageResponse;
import com.passportphoto.exceptions.ImageInvalidFormatException;
import com.passportphoto.service.AutomatePassportPhotoService;


/**
 * The {@code AutomatePassportPhotoController} provides endpoints to
 * automate passport photo generation using uploaded images and template rules.
 */
@RestController
@RequestMapping("/automate")
@CrossOrigin(origins = "http://localhost:5173")
public class AutomatePassportPhotoController {

    /** Service that processes the uploaded image into a passport photo */
    private final AutomatePassportPhotoService automatePassportPhotoService;

    /**
     * Constructs the controller with the automate photo service.
     *
     * @param automatePassportPhotoService service used for automation logic
     */
    public AutomatePassportPhotoController(AutomatePassportPhotoService automatePassportPhotoService) {
        this.automatePassportPhotoService = automatePassportPhotoService;
    }

    /**
     * Automates passport photo generation based on the uploaded image and selected template/country.
     *
     * @param file     the image file to be processed
     * @param country  the selected country code (optional)
     * @param template the template name (optional)
     * @return a ResponseEntity containing the result of the image processing
     */
    @PostMapping("/passportphoto")
    public ResponseEntity<AutomatedImageResponse> automatePassportPhoto(
        @RequestParam(value = "image", required = false) MultipartFile file,
        @RequestParam(value = "country", required = false) String country,
        @RequestParam(value = "template", required = false) String template
    ) { try {
            String processedBase64 = automatePassportPhotoService.automatePassportPhoto(file,country,template);
            return ResponseEntity.ok(new AutomatedImageResponse("success","Automated Generated Image",processedBase64));

        }catch (Exception e){
            return ResponseEntity.ok(new AutomatedImageResponse("failed","Automated Generated Image",null));
        }
            
    }

    /**
     * Automates passport photo generation in batch mode.
     *
     * @param fileList list of image files to be processed
     * @param country  the selected country code (optional)
     * @param template the template name (optional)
     * @return a ResponseEntity with base64-encoded images or error message
     */
    @PostMapping("/batch/passportphoto")
    public ResponseEntity<Map<String, String[]>> automatePassportPhoto(
        @RequestParam(value = "image", required = false) List<MultipartFile> fileList,
        @RequestParam(value = "country", required = false) String country,
        @RequestParam(value = "template", required = false) String template
    ) {
        try {
            String[] processedBase64 = automatePassportPhotoService.batchProcessing(fileList,country,template);
            Map<String, String[]> response = Collections.singletonMap("processedImage", (processedBase64));

            return ResponseEntity.ok(response);
        } catch (ImageInvalidFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", new String[]{e.getMessage()}));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", new String[]{"Image processing failed"}));
        }
    }

}
