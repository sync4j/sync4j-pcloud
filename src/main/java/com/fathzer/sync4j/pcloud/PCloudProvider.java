package com.fathzer.sync4j.pcloud;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.LongConsumer;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.HashAlgorithm;
import com.fathzer.sync4j.pcloud.internal.api.PCloud;
import com.fathzer.sync4j.pcloud.internal.api.PCloudAPI;
import com.google.gson.JsonObject;
import com.pcloud.sdk.RemoteEntry;
import com.pcloud.sdk.RemoteFile;
import com.pcloud.sdk.RemoteFolder;

public class PCloudProvider implements FileProvider {
    private final PCloud pcloud;

    public PCloudProvider(String accessToken) {
        this.pcloud = new PCloudAPI(accessToken);
    }

    @Override
    public List<HashAlgorithm> getSupportedHash() {
        return List.of(HashAlgorithm.SHA1);
    }

    @Override
    public boolean isFastListSupported() {
        return true;
    }

    @Override
    public Entry get(String path) throws IOException {
        try {
            RemoteEntry remoteEntry = this.pcloud.get(path);
            return remoteEntry.isFolder() ? new PcloudFolder(remoteEntry, this, false) : new PcloudFile(remoteEntry, this);
        } catch (FileNotFoundException e) {
            return new PcloudMissingFile(path, this);
        }
    }

    PcloudFolder getRemoteFolder(long folderId) throws IOException {
        return new PcloudFolder(this.pcloud.listFolder(folderId, false), this, false);
    }

    String getHash(RemoteFile remoteFile, HashAlgorithm hashAlgorithm) throws IOException {
        return this.pcloud.getHash(remoteFile, hashAlgorithm);
    }

    void delete(RemoteEntry remoteEntry) throws IOException {
        this.pcloud.delete(remoteEntry);
    }

    InputStream getInputStream(RemoteFile remoteFile) throws IOException {
        return this.pcloud.getInputStream(remoteFile);
    }

    RemoteFolder listFolder(long folderId, boolean recursive) throws IOException {
        return this.pcloud.listFolder(folderId, recursive);
    }

    JsonObject upload(long folderId, String fileName, File content, LongConsumer progressListener) throws IOException {
        return this.pcloud.upload(folderId, fileName, content.getInputStream(), content.getSize(), content.getLastModified(), content.getCreationTime(), progressListener);
    }
    
    @Override
    public void close() {
        this.pcloud.close();
    }
}
