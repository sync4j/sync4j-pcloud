package com.fathzer.sync4j.pcloud;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.HashAlgorithm;
import com.fathzer.sync4j.helper.AbstractFileProvider;

import com.fathzer.sync4j.helper.PathUtils;
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
public class PCloudProvider extends AbstractFileProvider {
    private final PCloud pcloud;
    private final String rootPath;

    /** Constructor.
     * @param zone the zone to use. See {@link Zone} for available zones.
     * @param accessToken the access token to use
     * @param rootPath the root path to use (e.g. "" for the pcloud account root folder, "/folder" for a subfolder)
     * @throws IOException if an I/O error occurs or if <code>rootPath</code> is not an existing folder
     */
    public PCloudProvider(@Nonnull Zone zone, @Nonnull String accessToken, @Nonnull String rootPath) throws IOException {
        // SHA1 is the only hash algorithm supported by all pCloud's zones
        super(true, List.of(HashAlgorithm.SHA1), true);
        this.checkPath(rootPath);
        this.rootPath = rootPath;
        this.pcloud = new PCloudAPI(zone, accessToken);
        try {
            if (!this.pcloud.get(rootPath).isFolder()) {
                throw new IOException("Root path " + rootPath + " is not a folder");
            }            
        } catch (IOException e) {
            this.pcloud.close();
            throw e;
        }
    }
    
    @Override
    public long getCreationTimePrecision() {
        return 999L;
    }
    
    @Override
    public long getLastModifiedTimePrecision() {
        return 999L;
    }
    
    @Override
    public Entry get(@Nonnull String path) throws IOException {
        try {
            this.checkPath(path);
            final RemoteEntry remoteEntry = this.pcloud.get(this.rootPath + path);
            if (FileProvider.ROOT_PATH.equals(path)) {
                return new PCloudFolder(null, null, remoteEntry, this, false);
            }
            final String parentPath = PathUtils.getParent(path);
            return remoteEntry.isFolder() ? new PCloudFolder(parentPath, null, remoteEntry, this, false) : new PCloudFile(parentPath, null, remoteEntry, this);
        } catch (FileNotFoundException e) {
            return new PCloudMissingFile(path, this);
        }
    }
    
    @Nonnull
    PCloud pCloud() {
        return this.pcloud;
    }

    void checkWriteOperationsAllowed() throws IOException {
        super.checkReadOnly();
    }
    
    @Override
    public void close() {
        this.pcloud.close();
    }
}
