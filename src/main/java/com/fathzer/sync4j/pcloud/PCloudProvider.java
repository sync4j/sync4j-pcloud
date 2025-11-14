package com.fathzer.sync4j.pcloud;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.HashAlgorithm;
import com.fathzer.sync4j.pcloud.internal.PathUtils;
import com.fathzer.sync4j.pcloud.internal.api.PCloud;
import com.fathzer.sync4j.pcloud.internal.api.PCloudAPI;
import com.pcloud.sdk.RemoteEntry;

import jakarta.annotation.Nonnull;

/**
 * pCloud provider for sync4j.
 * <br>
 * Please note that:
 * <ul>
 * <li>The only supported hash algorithm is SHA1.</li>
 * </ul>
 */
public class PCloudProvider implements FileProvider {
    private final PCloud pcloud;

    /** The root path that can be passed to {@link #get(String)}. */
    public static final String ROOT_PATH = "";

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
        // SHA1 is the only hash algorithm supported by all pCloud's zones
        return List.of(HashAlgorithm.SHA1);
    }

    @Override
    public boolean isFastListSupported() {
        return true;
    }

    @Override
    public Entry get(@Nonnull String path) throws IOException {
        try {
            final String parentPath = PathUtils.getParent(path);
            RemoteEntry remoteEntry = this.pcloud.get(path);
            return remoteEntry.isFolder() ? new PCloudFolder(parentPath, remoteEntry, this, false) : new PCloudFile(parentPath, remoteEntry, this);
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
