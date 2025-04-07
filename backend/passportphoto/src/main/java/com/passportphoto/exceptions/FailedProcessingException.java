package com.passportphoto.exceptions;

import com.passportphoto.exceptions.ImageException;

/**
 * Exception thrown when image processing fails.
 * Extends ImageException to provide a specific error type.
 */

public class FailedProcessingException extends ImageException {

     /**
     * Constructs a new FailedProcessingException with the specified message.
     * @param Message The error message describing the failure.
     */

    public FailedProcessingException(String Message, Exception e){
        super(Message);
    }
    
    
}
