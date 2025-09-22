package com.fathzer.sync4j.pcloud;

import com.fathzer.sync4j.File;
import com.pcloud.sdk.RemoteEntry;

public abstract class PcloudEntry implements File {
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
    public String getName() {
        return remoteEntry.name();
    }

    @Override
    public long getCreationTime() {
        return remoteEntry.created().getTime();
    }

    @Override
    public long getLastModified() {
        return remoteEntry.lastModified().getTime();
    }

    RemoteEntry getRemoteEntry() {
        return remoteEntry;
    }

    @Override
    public String toString() {
        return remoteEntry.toString();
    }
}
