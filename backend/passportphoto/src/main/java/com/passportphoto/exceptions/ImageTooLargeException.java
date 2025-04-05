package com.passportphoto.exceptions;

import com.passportphoto.exceptions.ImageException;

/**
 * Exception thrown when an image exceeds the maximum allowed size.
 * Extends ImageException to indicate size-related errors.
 */

public class ImageTooLargeException extends ImageException {

    /**
     * Constructs a new ImageTooLargeException with the specified message.
     * @param Message The error message describing the size limit violation.
     */
    public ImageTooLargeException(String Message){
        super(Message);
    }
    
    
}
