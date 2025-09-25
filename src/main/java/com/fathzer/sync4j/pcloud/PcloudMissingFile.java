package com.fathzer.sync4j.pcloud;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.fathzer.sync4j.Entry;

class PcloudMissingFile implements Entry {
    private final Path path;
    private final PCloudProvider provider;
    
    PcloudMissingFile(String name, PCloudProvider provider) {
        this.path = Paths.get(name);
        this.provider = provider;
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
    public Optional<Entry> getParent() throws IOException {
        final Path parent = path.getParent();
        if (parent == null) {
            return Optional.empty();
        }
        Entry parentEntry = provider.get(parent.toString());
        if (parentEntry.isFile()) {
            throw new IOException("Parent is a file");
        }
        return Optional.of(parentEntry);
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
}