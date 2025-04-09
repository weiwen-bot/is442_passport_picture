/*
 * AutomatedImageResponse.java
 * 
 * This class represents the response DTO for the automated image generation operation
 * in the Passport Picture Project.
 * 
 */

package com.passportphoto.dto;
 
/**
  * The {@code AutomatedImageResponse} class is a Data Transfer Object (DTO)
  * that encapsulates the result of an auto-generated image, including
  * a status string, a descriptive message, and the base64-encoded image data.
  */
 
public class AutomatedImageResponse extends ImageResponse {

    /**
     * Constructs an {@code AutomatedImageResponse} with the provided status,
     * message, and base64-encoded image string.
     *
     * @param status  response status ("success" or "error")
     * @param message descriptive status message
     * @param image   base64-encoded image string
     */
 
    public AutomatedImageResponse(String status, String message, String image) {
         super(status, message, image);
    }
 }