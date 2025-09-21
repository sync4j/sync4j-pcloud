package com.fathzer.sync4j.pcloud;

import java.io.IOException;
import java.util.List;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.HashAlgorithm;
import com.pcloud.sdk.RemoteEntry;

public class PcloudFolder extends PcloudEntry {
    private final boolean recursivlyLoaded;

    PcloudFolder(RemoteEntry remoteEntry, PCloudProvider provider, boolean recursivlyLoaded) {
        super(remoteEntry, provider);
        if (!remoteEntry.isFolder()) {
            throw new IllegalArgumentException("Not a folder");
        }
        this.recursivlyLoaded = recursivlyLoaded;
    }

    @Override
    public long getSize() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public String getHash(HashAlgorithm hashAlgorithm) throws IOException {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public List<File> list() throws IOException {
        if (!remoteEntry.isFolder()) {
            throw new IllegalArgumentException("Not a directory");
        }
        if (!this.recursivlyLoaded) {
            remoteEntry = provider.listFolder(remoteEntry.asFolder().folderId(), false);
        }
        return remoteEntry.asFolder().children().stream()
                .map(f -> (File)createEntry(f))
                .toList();
    }

    private PcloudEntry createEntry(RemoteEntry remoteEntry) {
        if (remoteEntry.isFile()) {
            return new PcloudFile(remoteEntry, provider);
        } else {
            return new PcloudFolder(remoteEntry, provider, this.recursivlyLoaded);
        }
    }
}
