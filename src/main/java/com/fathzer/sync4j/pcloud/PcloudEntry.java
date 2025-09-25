package com.fathzer.sync4j.pcloud;

import java.io.IOException;
import java.util.Optional;

import com.fathzer.sync4j.Entry;
import com.pcloud.sdk.RemoteEntry;

public abstract class PcloudEntry implements Entry {
    protected RemoteEntry remoteEntry;
    protected final PCloudProvider provider;

    protected PcloudEntry(RemoteEntry remoteEntry, PCloudProvider provider) {
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
        return Optional.of(provider.getRemoteFolder(parentFolderId));
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
        provider.delete(remoteEntry);
    }
}
