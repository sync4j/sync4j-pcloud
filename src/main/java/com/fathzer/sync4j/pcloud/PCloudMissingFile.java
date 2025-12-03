package com.fathzer.sync4j.pcloud;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.Folder;

import jakarta.annotation.Nonnull;

class PCloudMissingFile implements Entry {
    private final Path path;
    private final PCloudProvider provider;
    
    PCloudMissingFile(@Nonnull String fullPath, @Nonnull PCloudProvider provider) {
        this.path = Paths.get(fullPath);
        this.provider = provider;
    }

    @Override
    public FileProvider getFileProvider() {
        return provider;
    }
    
    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public boolean isFolder() {
        return false;
    }

    @Override
    public String getParentPath() {
        return path.getParent().toString();
    }

    @Override
    public Folder getParent() throws IOException {
        final Path parent = path.getParent();
        if (parent == null) {
            return null;
        }
        Entry parentEntry = provider.get(parent.toString());
        if (!parentEntry.isFolder()) {
            throw new IOException("Parent is not a folder");
        }
        return parentEntry.asFolder();
    }

    @Override
    public String getName() {
        Path fileName = path.getFileName();
        return fileName == null ? "" : fileName.toString();
    }

    @Override
    public boolean exists() {
        return false;
    }
    
    @Override
    public void delete() {
        // Do nothing, file is already deleted
    }

    @Override
    public String toString() {
        return "pCloud:" + path.toAbsolutePath().toString();
    }
}