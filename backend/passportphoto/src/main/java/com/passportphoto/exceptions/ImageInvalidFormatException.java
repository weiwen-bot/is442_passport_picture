package com.passportphoto.exceptions;

/**
 * Exception thrown when an image has an invalid format.
 * Extends ImageException to indicate format-related errors.
 */

public class ImageInvalidFormatException extends ImageException {

    /**
     * Constructs a new ImageInvalidFormatException with the specified message.
     * @param Message The error message describing the format issue.
     */
    public ImageInvalidFormatException(String Message){
        super(Message);
    }
    
    
}
