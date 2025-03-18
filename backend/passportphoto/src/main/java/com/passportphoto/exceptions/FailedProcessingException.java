package com.passportphoto.exceptions;

import com.passportphoto.exceptions.ImageException;

public class FailedProcessingException extends ImageException {
    public FailedProcessingException(String Message){
        super(Message);
    }
    
    
}
