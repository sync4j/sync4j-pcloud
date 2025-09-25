package com.fathzer.sync4j.pcloud;

import java.io.IOException;
import java.util.List;
import java.util.function.LongConsumer;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;
import com.pcloud.sdk.RemoteEntry;
import com.pcloud.sdk.RemoteFolder;

class PcloudFolder extends PcloudEntry implements Folder {
    private boolean recursivlyLoaded;

    PcloudFolder(RemoteEntry remoteEntry, PCloudProvider provider, boolean recursivlyLoaded) {
        super(remoteEntry, provider);
        if (!remoteEntry.isFolder()) {
            throw new IllegalArgumentException("Not a folder");
        }
        this.recursivlyLoaded = recursivlyLoaded;
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    @Override
    public Folder preload() throws IOException {
        if (!remoteEntry.isFolder()) {
            throw new IllegalArgumentException("Not a directory");
        }
        if (!this.recursivlyLoaded) {
            remoteEntry = provider.listFolder(remoteEntry.asFolder().folderId(), true);
            this.recursivlyLoaded = true;
        }
        return this;
    }

    @Override
    public List<Entry> list() throws IOException {
        if (!remoteEntry.isFolder()) {
            throw new IllegalArgumentException("Not a directory");
        }
        if (!this.recursivlyLoaded) {
            remoteEntry = provider.listFolder(remoteEntry.asFolder().folderId(), false);
        }
        return remoteEntry.asFolder().children().stream()
                .map(f -> (Entry)createEntry(f))
                .toList();
    }

    private PcloudEntry createEntry(RemoteEntry remoteEntry) {
        if (remoteEntry.isFile()) {
            return new PcloudFile(remoteEntry, provider);
        } else {
            return new PcloudFolder(remoteEntry, provider, this.recursivlyLoaded);
        }
    }

    @Override
    public File copy(String fileName, File content, LongConsumer progressListener) throws IOException {
    	return new PcloudFile(provider.upload(remoteEntry.asFolder().folderId(), fileName, content, progressListener), provider);
    }

    @Override
    public Folder mkdir(String folderName) throws IOException {
        final RemoteFolder remoteFolder = provider.mkdir(remoteEntry.asFolder().folderId(), folderName);
        return new PcloudFolder(remoteFolder, provider, false);
    }
}
