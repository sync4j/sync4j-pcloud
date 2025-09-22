package com.fathzer.sync4j.pcloud;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.HashAlgorithm;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pcloud.sdk.ApiClient;
import com.pcloud.sdk.ApiError;
import com.pcloud.sdk.Authenticators;
import com.pcloud.sdk.PCloudSdk;
import com.pcloud.sdk.RemoteEntry;
import com.pcloud.sdk.RemoteFile;
import com.pcloud.sdk.RemoteFolder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

//Note IOException encapsulate APIError that are described in the pCloud API documentation (https://docs.pcloud.com/errors/index.html)
public class PCloudProvider implements FileProvider {
    private final ApiClient apiClient;
    private final URI apiURI;
    private final String token;
    private OkHttpClient httpClient;

    public PCloudProvider(String accessToken) {
        this.apiClient = PCloudSdk.newClientBuilder()
                .authenticator(Authenticators.newOAuthAuthenticator(accessToken))
                .create();
        this.apiURI = URI.create("https://" + this.apiClient.apiHost());
        this.token = accessToken;
    }

    @Override
    public List<HashAlgorithm> getSupportedHash() {
        return List.of(HashAlgorithm.SHA1);
    }

    private OkHttpClient getClient() {
        if (this.httpClient == null) {
            this.httpClient = new OkHttpClient();
        }
        return this.httpClient;
    }

    @Override
    public boolean isFastListSupported() {
        return true;
    }

    @Override
    public Entry get(String path, boolean fastList) throws IOException {
        try {
            return execute(() -> {
                RemoteEntry remoteFile = this.apiClient.loadFile(path).execute();
                if (remoteFile.isFolder()) {
                    remoteFile = this.apiClient.loadFolder(path).execute();
                    // Warning, loadFolder does not load the full folder content, if you want to get the children, you have to call listFolder
                    // or children() may return only the sub folders, not the files!
                    if (fastList) {
                        // Load the full tree folder content
                        remoteFile = listFolder(remoteFile.asFolder().folderId(), true);
                    }
                    return new PcloudFolder(remoteFile, this, fastList);
                }
                return new PcloudFile(remoteFile, this);
            });
        } catch (FileNotFoundException e) {
            return new PcloudMissingFile(path);
        }
    }

    @FunctionalInterface
    private interface PcloudCall<T> {
        T call() throws IOException, ApiError;
    }
    
    private <T> T execute(PcloudCall<T> call) throws IOException {
        try {
            return call.call();
        } catch (ApiError e) {
            if (e.errorCode() == 2055) {
                throw new FileNotFoundException(e.errorMessage());
            }
            throw new IOException(e);
        }
    }

    String getHash(RemoteFile remoteFile, HashAlgorithm hashAlgorithm) throws IOException {
        Objects.requireNonNull(hashAlgorithm);
        if (hashAlgorithm != HashAlgorithm.SHA1) {
            throw new IllegalArgumentException("Unsupported hash algorithm: " + hashAlgorithm);
        }
        final URI fileURI = this.apiURI.resolve("checksumfile?fileid=" + remoteFile.fileId());
        return getJson(fileURI.toURL()).get("sha1").getAsString();
    }

    JsonObject getJson(URL fileURI) throws IOException {
        final Request request = new Request.Builder()
                .url(fileURI)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + this.token)
                .build();
        try (Response response = this.getClient().newCall(request).execute()) {
            final JsonObject jsonResponse = JsonParser.parseString(response.body().string()).getAsJsonObject();
            // Check for API errors
            if (jsonResponse.has("error")) {
                throw new IOException("API error: " + jsonResponse.get("error").getAsString());
            }
            return jsonResponse;
        }
    }

    RemoteFolder listFolder(long folderId, boolean recursive) throws IOException {
        return execute(() -> this.apiClient.listFolder(folderId, recursive).execute());
    }
    
    @Override
    public void close() {
        apiClient.shutdown();
        if (this.httpClient != null) {
            this.httpClient.dispatcher().executorService().shutdown();
            this.httpClient.connectionPool().evictAll();
        }
    }
}
