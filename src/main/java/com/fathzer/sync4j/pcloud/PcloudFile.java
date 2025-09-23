package com.fathzer.sync4j.pcloud;

import java.io.IOException;
import java.io.InputStream;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.HashAlgorithm;
import com.pcloud.sdk.RemoteEntry;

public class PcloudFile extends PcloudEntry implements File {

    PcloudFile(RemoteEntry remoteEntry, PCloudProvider provider) {
        super(remoteEntry, provider);
        if (!remoteEntry.isFile()) {
            throw new IllegalArgumentException("Not a file");
        }
    }

    @Override
    public long getSize() {
        return remoteEntry.asFile().size();
    }

    @Override
    public long getCreationTime() {
        return remoteEntry.created().getTime();
    }

    @Override
    public long getLastModified() {
        return remoteEntry.lastModified().getTime();
    }

    @Override
    public boolean isFile() {
        return true;
    }

    @Override
    public boolean isFolder() {
        return false;
    }

    @Override
    public String getHash(HashAlgorithm hashAlgorithm) throws IOException {
        return provider.getHash(remoteEntry.asFile(), hashAlgorithm);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return provider.getInputStream(remoteEntry.asFile());
    }
}
