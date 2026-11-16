package com.fathzer.sync4j.pcloud;

/**
 * The configuration of a pCloud provider.
 * @param zone the zone of the pCloud account ("us" or "eu" in a configuration file, it is not case sensitive)
 * @param token the access token
 */
public record PCloudConfig(Zone zone, String token) {
    @Override
    public String toString() {
        // The token is a secret: it should not appear in a log
        return "PCloudConfig[zone=" + zone + ", token=***]";
    }
}
