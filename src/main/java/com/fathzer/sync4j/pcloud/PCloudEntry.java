package com.fathzer.sync4j.pcloud;

import java.io.IOException;
import java.util.Objects;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.helper.PathUtils;
import com.pcloud.sdk.RemoteEntry;

import jakarta.annotation.Nonnull;

abstract class PCloudEntry implements Entry {
    protected final String parentPath;
    protected RemoteEntry remoteEntry;
    protected final PCloudProvider provider;

    protected PCloudEntry(String parentPath, @Nonnull RemoteEntry remoteEntry, @Nonnull PCloudProvider provider) {
    	if (parentPath!=null) provider.checkPath(parentPath);
        this.parentPath = parentPath;
        this.remoteEntry = Objects.requireNonNull(remoteEntry);
        this.provider = Objects.requireNonNull(provider);
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
    public Entry getParent() throws IOException {
        if (isRoot()) {
            return null;
        }
        final long parentFolderId = remoteEntry.parentFolderId();
        final String gfPath = PathUtils.getParent(this.parentPath);
        return new PCloudFolder(gfPath, provider.pCloud().listFolder(parentFolderId, false), this.provider, false);
    }
    
    @Override
    public String getName() {
        return parentPath == null ? FileProvider.ROOT_PATH : remoteEntry.name();
    }

    RemoteEntry getRemoteEntry() {
        return remoteEntry;
    }
    
    private boolean isRoot() {
    	return parentPath == null;
    }

    @Override
    public void delete() throws IOException {
        provider.checkWriteOperationsAllowed();
        if (isRoot()) {
            throw new IOException("Cannot delete root folder");
        }
        provider.pCloud().delete(remoteEntry);
    }
    
    String fullPath() {
        return (parentPath == null ? "" : parentPath+ "/") + getName();
    }

    @Override
    public String toString() {
        return "pCloud:" + fullPath();
    }
}
