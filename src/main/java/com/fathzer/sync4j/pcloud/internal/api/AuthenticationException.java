package com.fathzer.sync4j.pcloud.internal.api;

import java.io.IOException;

class AuthenticationException extends IOException {
    private static final long serialVersionUID = 1L;
    
    AuthenticationException(String message) {
        super(message);
    }
}
