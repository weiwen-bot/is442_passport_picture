package com.passportphoto.exceptions;

/**
 * Base class for all image-related exceptions.
 * Extends RuntimeException to allow unchecked exceptions.
 */

 public class ImageException extends RuntimeException {

    /**
     * Constructs a new ImageException with the specified message.
     * @param Message The error message describing the issue.
     */
    public ImageException(String Message){
        super(Message);
    }
    
    
}
