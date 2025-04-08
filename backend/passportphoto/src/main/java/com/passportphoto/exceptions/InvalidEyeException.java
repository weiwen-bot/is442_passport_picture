package com.passportphoto.exceptions;

import com.passportphoto.exceptions.ImageException;

/**
 * Exception thrown when an image has an invalid format.
 * Extends ImageException to indicate format-related errors.
 */

public class InvalidEyeException extends FaceDetectionException {

    /**
     * Constructs a new ImageInvalidFormatException with the specified message.
     * @param Message The error message describing the format issue.
     */
    public InvalidEyeException(String Message){
        super(Message);
    }
    
    
}
