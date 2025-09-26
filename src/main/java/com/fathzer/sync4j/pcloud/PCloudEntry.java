package com.fathzer.sync4j.pcloud;

import java.io.IOException;
import java.util.Optional;

import com.fathzer.sync4j.Entry;
import com.pcloud.sdk.RemoteEntry;

import jakarta.annotation.Nonnull;

abstract class PCloudEntry implements Entry {
    protected RemoteEntry remoteEntry;
    protected final PCloudProvider provider;

    protected PCloudEntry(@Nonnull RemoteEntry remoteEntry, @Nonnull PCloudProvider provider) {
        this.remoteEntry = remoteEntry;
        this.provider = provider;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public Optional<Entry> getParent() throws IOException {
        if (isFolder()) {
            long folderId = remoteEntry.asFolder().folderId();
            if (folderId == 0) {
                return Optional.empty();
            }
        }
        final long parentFolderId = remoteEntry.parentFolderId();
        if (parentFolderId == 0) {
            return Optional.empty();
        }
        PCloudFolder parentFolder = new PCloudFolder(provider.pCloud().listFolder(parentFolderId, false), this.provider, false);
        return Optional.of(parentFolder);
    }
    
    @Override
    public String getName() {
        return remoteEntry.name();
    }

    RemoteEntry getRemoteEntry() {
        return remoteEntry;
    }

    @Override
    public String toString() {
        return remoteEntry.toString();
    }

    @Override
    public void delete() throws IOException {
        provider.pCloud().delete(remoteEntry);
    }
}
