package com.fathzer.sync4j.pcloud;

import java.io.IOException;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.helper.PathUtils;

import jakarta.annotation.Nonnull;

class PCloudMissingFile implements Entry {
    private final String path;
    private final PCloudProvider provider;
    
    PCloudMissingFile(@Nonnull String fullPath, @Nonnull PCloudProvider provider) {
        this.path = fullPath;
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
    public Entry getParent() throws IOException {
        return provider.get(PathUtils.getParent(path));
    }

    @Override
    public String getName() {
    	return PathUtils.getName(path);
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
        return "pCloud:" + path;
    }
}