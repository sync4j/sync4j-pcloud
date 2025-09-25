package com.fathzer.sync4j.pcloud;

import java.net.URI;

public enum Zone {
    // Europ first <= Trump is right, my country first!
    EU(URI.create("https://eapi.pcloud.com")),
    US(URI.create("https://api.pcloud.com"));
    
    private final URI uri;
    
    private Zone(URI uri) {
        this.uri = uri;
    }

    public URI getRootURI() {
        return this.uri;
    }
}