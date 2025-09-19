package com.fathzer.sync4j.pcloud;

import com.fathzer.sync4j.File;
import com.pcloud.sdk.RemoteFile;

public class PcloudFile implements File {
    private final RemoteFile remoteFile;

    public PcloudFile(RemoteFile remoteFile) {
        this.remoteFile = remoteFile;
    }

    @Override
    public String getName() {
        return remoteFile.name();
    }

    @Override
    public boolean isFile() {
        return remoteFile.isFile();
    }

    RemoteFile getRemoteFile() {
        return remoteFile;
    }
}
