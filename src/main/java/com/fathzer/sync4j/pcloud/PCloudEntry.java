package com.fathzer.sync4j.pcloud;

import java.io.IOException;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.pcloud.internal.PathUtils;
import com.pcloud.sdk.RemoteEntry;

import jakarta.annotation.Nonnull;

abstract class PCloudEntry implements Entry {
    protected final String parentPath;
    protected RemoteEntry remoteEntry;
    protected final PCloudProvider provider;

    protected PCloudEntry(@Nonnull String parentPath, @Nonnull RemoteEntry remoteEntry, @Nonnull PCloudProvider provider) {
        this.parentPath = parentPath;
        this.remoteEntry = remoteEntry;
        this.provider = provider;
    }

    @Override
    public FileProvider getFileProvider() {
        return provider;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public String getParentPath() {
        return parentPath;
    }

    @Override
    public Entry getParent() throws IOException {
        if (isFolder()) {
            long folderId = remoteEntry.asFolder().folderId();
            if (folderId == 0) {
                return null;
            }
        }
        final long parentFolderId = remoteEntry.parentFolderId();
        final String gfPath = PathUtils.getParent(this.parentPath);
        return new PCloudFolder(gfPath, provider.pCloud().listFolder(parentFolderId, false), this.provider, false);
    }
    
    @Override
    public String getName() {
        return parentPath == null ? PCloudProvider.ROOT_PATH : remoteEntry.name();
    }

    RemoteEntry getRemoteEntry() {
        return remoteEntry;
    }

    @Override
    public void delete() throws IOException {
        provider.pCloud().delete(remoteEntry);
    }

    @Override
    public String toString() {
        return "pCloud:" + (parentPath == null ? "" : parentPath + "/") + getName();
    }
}
