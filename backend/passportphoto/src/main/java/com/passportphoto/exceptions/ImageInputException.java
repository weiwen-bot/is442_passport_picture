package com.passportphoto.exceptions;

import com.passportphoto.exceptions.ImageException;

/**
 * Exception thrown when reading image fails.
 * Extends ImageException to indicate image errors.
 */

public class ImageInputException extends ImageException {

    /**
     * Constructs a new ImageInputException with the specified message.
     * @param Message The error message describing the format issue.
     */
    public ImageInputException(String Message){
        super(Message);
    }
    public ImageInputException(String Message, Exception e){
        super(Message);
    }
    
    
}
