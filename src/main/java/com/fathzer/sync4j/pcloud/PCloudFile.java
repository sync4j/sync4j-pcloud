package com.fathzer.sync4j.pcloud;

import java.io.IOException;
import java.io.InputStream;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.HashAlgorithm;
import com.pcloud.sdk.RemoteEntry;

import jakarta.annotation.Nonnull;

class PCloudFile extends PCloudEntry implements File {

    PCloudFile(@Nonnull String parentPath, @Nonnull RemoteEntry remoteEntry, @Nonnull PCloudProvider provider) {
        super(parentPath, remoteEntry, provider);
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
    public long getLastModifiedTime() {
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
        return provider.pCloud().getHash(remoteEntry.asFile(), hashAlgorithm);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return provider.pCloud().getInputStream(remoteEntry.asFile());
    }
}
