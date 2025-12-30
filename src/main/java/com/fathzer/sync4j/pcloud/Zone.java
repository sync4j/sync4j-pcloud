package com.fathzer.sync4j.pcloud;

import java.net.URI;

import jakarta.annotation.Nonnull;

/**
 * pCloud zone.
 * <br>
 * PCloud uses totally independent API for each zone.
 * There is no way to securely detect the zone from the access token.
 * <br>
 * This enum is used to select the zone when creating a new instance of {@link PCloudProvider}.
 */
public enum Zone {
    /**
     * European zone.
     */
    EU(URI.create("https://eapi.pcloud.com")),
    /**
     * US zone.
     */
    US(URI.create("https://api.pcloud.com"));
    
    private final URI uri;
    
    private Zone(URI uri) {
        this.uri = uri;
    }

    /**
     * Returns the root URI of the zone's http API.
     * @return an API
     */
    @Nonnull
    public URI getRootURI() {
        return this.uri;
    }
}