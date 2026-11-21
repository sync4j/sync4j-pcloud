package com.fathzer.sync4j.pcloud;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.List;
import java.util.function.LongConsumer;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.Folder;
import com.pcloud.sdk.RemoteEntry;
import com.pcloud.sdk.RemoteFile;
import com.pcloud.sdk.RemoteFolder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

class PCloudFolder extends PCloudEntry implements Folder {
    private boolean recursivlyLoaded;

    /**
     * Constructor.
     * @param parentPath the path of the parent folder, null if the entry is the root.
     * @param parent the parent entry, null if the parent entry is unknown (typically if created by a direct get) or if the entry is the root.
     * @param remoteEntry the remote entry.
     * @param provider the provider.
     * @throws IllegalArgumentException if parentPath is invalid or, parentPath is null and parent != null
     */
    PCloudFolder(@Nullable String parentPath, @Nullable Folder parent, @Nonnull RemoteEntry remoteEntry, @Nonnull PCloudProvider provider, boolean recursivlyLoaded) {
        super(parentPath, parent, remoteEntry, provider);
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
            throw new IllegalStateException("Not a directory");
        }
        if (!this.recursivlyLoaded) {
            remoteEntry = provider.pCloud().listFolder(remoteEntry.asFolder().folderId(), true);
            this.recursivlyLoaded = true;
        }
        return this;
    }

    @Override
    public List<Entry> list() throws IOException {
        if (!remoteEntry.isFolder()) {
            throw new IllegalStateException("Not a directory");
        }
        if (!this.recursivlyLoaded) {
            remoteEntry = provider.pCloud().listFolder(remoteEntry.asFolder().folderId(), false);
        }
        return remoteEntry.asFolder().children().stream()
                .map(f -> (Entry)createEntry(f))
                .toList();
    }

    private PCloudEntry createEntry(RemoteEntry remoteEntry) {
        if (remoteEntry.isFile()) {
            return new PCloudFile(fullPath(), this, remoteEntry, provider);
        } else {
            return new PCloudFolder(fullPath(), this, remoteEntry, provider, this.recursivlyLoaded);
        }
    }

    @Override
    public File copy(String fileName, File content, LongConsumer progressListener) throws IOException {
        provider.checkWriteOperationsAllowed();
        checkFileName(fileName);
    	return new PCloudFile(fullPath(), this, upload(remoteEntry.asFolder().folderId(), fileName, content, progressListener), provider);
    }

    private RemoteFile upload(long folderId, String fileName, File content, LongConsumer progressListener) throws IOException {
        return provider.pCloud().upload(folderId, fileName, content.getInputStream(), content.getSize(), content.getLastModifiedTime(), content.getCreationTime(), progressListener);
    }

    @Override
    public Folder mkdir(String folderName) throws IOException {
        provider.checkWriteOperationsAllowed();
        checkFileName(folderName);
        // pCloud accepts a file and a folder with the same name, but a FileProvider exposes a strict tree.
        // The check is made with a single call by path, whatever the size of this folder. It also saves the creation call if the folder already exists.
        // False positive: "/" is the path separator of the sync4j paths (see FileProvider), it does not depend on the operating system
        @SuppressWarnings("java:S1075")
        final String childPath = fullPath() + "/" + folderName;
        final Entry existing = provider.get(childPath);
        if (existing.isFile()) {
            throw new FileAlreadyExistsException(childPath, null, "A file with the same name already exists");
        } else if (existing.isFolder()) {
            // mkdir is idempotent
            return existing.asFolder();
        }
        final RemoteFolder remoteFolder = provider.pCloud().mkdir(remoteEntry.asFolder().folderId(), folderName);
        return new PCloudFolder(fullPath(), this, remoteFolder, provider, false);
    }
}
