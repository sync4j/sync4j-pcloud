package com.fathzer.sync4j.pcloud.internal.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.LongConsumer;

import com.fathzer.sync4j.HashAlgorithm;
import com.pcloud.sdk.RemoteEntry;
import com.pcloud.sdk.RemoteFile;
import com.pcloud.sdk.RemoteFolder;

public interface PCloud extends AutoCloseable {

    RemoteEntry get(String path) throws IOException;

    String getHash(RemoteFile remoteFile, HashAlgorithm hashAlgorithm) throws IOException;

    InputStream getInputStream(RemoteFile remoteFile) throws IOException;

    RemoteFolder listFolder(long folderId, boolean recursive) throws IOException;

    void delete(RemoteEntry remoteEntry) throws IOException;

    RemoteFile upload(long folderId, String fileName, InputStream content, long size, long mtime, long ctime, LongConsumer progressListener) throws IOException;

    RemoteFolder mkdir(long folderId, String folderName) throws IOException;
    
    @Override
    void close();
}
