package com.fathzer.sync4j.pcloud;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.HashAlgorithm;

public class PcloudMissingFile implements File {
    private final String name;
    
    public PcloudMissingFile(String name) {
        this.name = name;
    }
    
    @Override
    public boolean isFile() {
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
    public long getSize() {
        return 0;
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public String getHash(HashAlgorithm hashAlgorithm) throws IOException {
        throw new FileNotFoundException();
    }

    @Override
    public List<File> list() throws IOException {
        throw new FileNotFoundException();
    }
}