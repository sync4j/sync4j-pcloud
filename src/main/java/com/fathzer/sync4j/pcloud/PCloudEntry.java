package com.fathzer.sync4j.pcloud;

import java.io.IOException;
import java.util.Objects;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.helper.PathUtils;
import com.pcloud.sdk.ApiError;
import com.pcloud.sdk.RemoteEntry;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

abstract class PCloudEntry implements Entry {
    protected final String parentPath;
    protected RemoteEntry remoteEntry;
    protected final PCloudProvider provider;
    protected Folder parent;

    /**
     * Constructor.
     * @param parentPath the path of the parent folder, null if the entry is the root.
     * @param parent the parent entry, null if the parent entry is unknown (typically if created by a direct get) or if the entry is the root.
     * @param remoteEntry the remote entry.
     * @param provider the provider.
     * @throws IllegalArgumentException if parentPath is invalid or, parentPath is null and parent != null
     */
    protected PCloudEntry(@Nullable String parentPath , @Nullable Folder parent, @Nonnull RemoteEntry remoteEntry, @Nonnull PCloudProvider provider) {
    	if (parentPath!=null) {
            provider.checkPath(parentPath);
        } else if (parent!=null) {
            throw new IllegalArgumentException("Parent entry must be null if parent is null");
        }
        this.parentPath = parentPath;
        this.parent = parent;
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
        if (parent==null) {
            final long parentFolderId = remoteEntry.parentFolderId();
            final String gfPath = PathUtils.getParent(this.parentPath);
            parent = new PCloudFolder(gfPath, null,provider.pCloud().listFolder(parentFolderId, false), this.provider, false);
        }
        return parent;
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
        try {
        	provider.pCloud().delete(remoteEntry);
        } catch (IOException e) {
        	// Ignore api errors caused by previous deletion 
            if (!isEntryAlreadyDeletedError(e)) {
                throw e;
            }
        }
    }
    
    private boolean isEntryAlreadyDeletedError(IOException e) {
        Throwable cause = e.getCause();
        return (cause instanceof ApiError apiError && apiError.errorCode() == 2005);
    }
    
    String fullPath() {
        return (parentPath == null ? "" : parentPath+ "/") + getName();
    }

    @Override
    public String toString() {
        return "pCloud:" + fullPath();
    }
}
