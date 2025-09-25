package com.fathzer.sync4j.pcloud.internal.api;

import java.io.IOException;

public class AuthenticationException extends IOException {
    private static final long serialVersionUID = 1L;
    
    public AuthenticationException(String message) {
        super(message);
    }
}
