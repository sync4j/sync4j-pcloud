package com.fathzer.sync4j.pcloud;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.io.InputStream;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.helper.PathUtils;
import com.fathzer.sync4j.test.UnderlyingFileSystem;
import com.pcloud.sdk.ApiClient;
import com.pcloud.sdk.ApiError;
import com.pcloud.sdk.Call;
import com.pcloud.sdk.DataSource;
import com.pcloud.sdk.RemoteFile;

class PCloudFileSystem implements UnderlyingFileSystem {
    ApiClient apiClient;
    String rootPath;

    public PCloudFileSystem(ApiClient apiClient, String rootPath) {
        this.apiClient = apiClient;
        this.rootPath = rootPath;
    }
    
    private <T> T execute(Call<T> call) throws IOException {
        try {
            return call.execute();
        } catch (ApiError e) {
            throw new IOException(e);
        }
    }

    private String absolute(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return rootPath + "/" + path;
    }

    @Override
    public void createFile(String path) throws IOException {
        execute(apiClient.createFile(PathUtils.getParent(absolute(path)), PathUtils.getName(path), DataSource.create("content".getBytes())));
    }

    @Override
    public void deleteFile(String path) throws IOException {
        execute(apiClient.deleteFile(absolute(path)));
    }

    @Override
    public void deleteFolder(String path) throws IOException {
        execute(apiClient.deleteFolder(absolute(path)));
    }

    @Override
    public void createFolder(String path) throws IOException {
        execute(apiClient.createFolder(absolute(path)));
    }

    @Override
    public void assertUnderlyingFileEquals(String path, File file) throws IOException {
        RemoteFile remoteFile = execute(apiClient.loadFile(absolute(path)));
        try (InputStream is = remoteFile.byteStream(); InputStream fileIs = file.getInputStream()) {
            byte[] remoteFileBytes = is.readAllBytes();
            byte[] fileBytes = fileIs.readAllBytes();
            assertArrayEquals(remoteFileBytes, fileBytes);
        } catch (ApiError e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean underlyingFolderExists(String path) throws IOException {
        try {
            return execute(apiClient.loadFolder(absolute(path))).isFolder();
        } catch (IOException e) {
            Throwable cause = e.getCause();
            // 2005 is the error code for "not found"
            if ((cause instanceof ApiError error) && error.errorCode() == 2005) {
                return false;
            }
            throw e;
        }
    }
}
