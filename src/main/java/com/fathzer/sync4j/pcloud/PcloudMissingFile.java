package com.fathzer.sync4j.pcloud;

import com.fathzer.sync4j.Entry;

public class PcloudMissingFile implements Entry {
    private final String name;
    
    public PcloudMissingFile(String name) {
        this.name = name;
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
    public String getName() {
        return name;
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