package com.fathzer.sync4j.pcloud;

import java.io.IOException;
import java.util.List;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.HashAlgorithm;
import com.pcloud.sdk.RemoteEntry;

public class PcloudFile extends PcloudEntry {

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
    public boolean isFile() {
        return remoteEntry.isFile();
    }

    @Override
    public String getHash(HashAlgorithm hashAlgorithm) throws IOException {
        return provider.getHash(remoteEntry.asFile(), hashAlgorithm);
    }

    @Override
    public List<File> list() throws IOException {
        throw new UnsupportedOperationException("Not supported");
    }
}
