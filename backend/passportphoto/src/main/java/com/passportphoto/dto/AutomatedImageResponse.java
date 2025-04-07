/*
 * AutomatedImageResponse.java
 * 
 * This class represents the response DTO for the automated image generate operation
 * in the Passport Picture Project.
 * 
 */

 package com.passportphoto.dto;

 import com.passportphoto.dto.ImageResponse;
 
 /**
  * The {@code AutomatedImageResponse} class is a Data Transfer Object (DTO)
  * that encapsulates the result of an auto generated image, including
  * a status string, a descriptive message, and the base64-encoded image data.
  */
 
  public class AutomatedImageResponse extends ImageResponse {
 
 
     public AutomatedImageResponse(String status, String message, String image) {
         super(status, message, image);
     }
 }
 