package com.fathzer.sync4j.pcloud;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.HashAlgorithm;
import com.pcloud.sdk.RemoteEntry;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

class PCloudFile extends PCloudEntry implements File {
    /**
     * Constructor.
     * @param parentPath the path of the parent folder, null if the entry is the root.
     * @param parent the parent entry, null if the parent entry is unknown (typically if created by a direct get) or if the entry is the root.
     * @param remoteEntry the remote entry.
     * @param provider the provider.
     * @throws IllegalArgumentException if parentPath is invalid or, parentPath is null and parent != null
     */
    PCloudFile(@Nonnull String parentPath, @Nullable Folder parent, @Nonnull RemoteEntry remoteEntry, @Nonnull PCloudProvider provider) {
        super(parentPath, parent, remoteEntry, provider);
        Objects.requireNonNull(parentPath);
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
