package com.passportphoto.exceptions;

import com.passportphoto.exceptions.ImageException;

public class ImageTooLargeException extends ImageException {
    public ImageTooLargeException(String Message){
        super(Message);
    }
    
    
}
