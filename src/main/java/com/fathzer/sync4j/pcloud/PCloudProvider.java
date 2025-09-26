package com.fathzer.sync4j.pcloud;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.HashAlgorithm;
import com.fathzer.sync4j.pcloud.internal.api.PCloud;
import com.fathzer.sync4j.pcloud.internal.api.PCloudAPI;
import com.pcloud.sdk.RemoteEntry;

import jakarta.annotation.Nonnull;

/**
 * pCloud provider for sync4j.
 * <br>
 * Please note that:
 * <ul>
 * <li>All paths should start with a slash, root folder is "/".</li>
 * <li>The only supported hash algorithm is SHA1.</li>
 * </ul>
 */
public class PCloudProvider implements FileProvider {
    private final PCloud pcloud;

    /** Constructor.
     * @param zone the zone to use. See {@link Zone} for available zones.
     * @param accessToken the access token to use
     * @throws IOException if an I/O error occurs
     */
    public PCloudProvider(@Nonnull Zone zone, @Nonnull String accessToken) throws IOException {
        this.pcloud = new PCloudAPI(zone, accessToken);
    }

    @Override
    public List<HashAlgorithm> getSupportedHash() {
        return List.of(HashAlgorithm.SHA1);
    }

    @Override
    public boolean isFastListSupported() {
        return true;
    }

    @Override
    public Entry get(@Nonnull String path) throws IOException {
        try {
            RemoteEntry remoteEntry = this.pcloud.get(path);
            return remoteEntry.isFolder() ? new PCloudFolder(remoteEntry, this, false) : new PCloudFile(remoteEntry, this);
        } catch (FileNotFoundException e) {
            return new PCloudMissingFile(path, this);
        }
    }
    
    @Nonnull
    PCloud pCloud() {
        return this.pcloud;
    }
    
    @Override
    public void close() {
        this.pcloud.close();
    }
}
