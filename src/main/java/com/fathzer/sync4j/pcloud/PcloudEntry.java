package com.fathzer.sync4j.pcloud;

import java.io.IOException;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.HashAlgorithm;
import com.pcloud.sdk.RemoteEntry;

public abstract class PcloudEntry implements File {
    protected RemoteEntry remoteEntry;
    protected final PCloudProvider provider;

    protected PcloudEntry(RemoteEntry remoteEntry, PCloudProvider provider) {
        this.remoteEntry = remoteEntry;
        this.provider = provider;
    }

    @Override
    public String getName() {
        return remoteEntry.name();
    }

    private void checkFile() {
        if (!remoteEntry.isFile()) {
            throw new IllegalArgumentException("Not a file");
        }
    }

    @Override
    public long getLastModified() {
        return remoteEntry.lastModified().getTime();
    }

    @Override
    public boolean isFile() {
        return remoteEntry.isFile();
    }

    @Override
    public String getHash(HashAlgorithm hashAlgorithm) throws IOException {
        checkFile();
        return provider.getHash(remoteEntry.asFile(), hashAlgorithm);
    }

    RemoteEntry getRemoteEntry() {
        return remoteEntry;
    }

    @Override
    public String toString() {
        return remoteEntry.toString();
    }
}
