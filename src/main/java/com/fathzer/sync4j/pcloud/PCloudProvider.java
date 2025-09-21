package com.fathzer.sync4j.pcloud;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;

import com.fathzer.sync4j.File;
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

//Note IOException encapsulate APIError that are described in the pCloud API documentation (https://docs.pcloud.com/errors/index.html)
public class PCloudProvider implements FileProvider<PcloudEntry> {
    private final ApiClient apiClient;
    private final URI apiURI;
    private final String token;
    private HttpClient httpClient;

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

    private HttpClient getClient() {
        if (this.httpClient == null) {
            this.httpClient = HttpClient.newHttpClient();
        }
        return this.httpClient;
    }

    @Override
    public boolean isFastListSupported() {
        return true;
    }

    @Override
    public File get(String path, boolean fastList) throws IOException {
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
        return getJson(fileURI).get("sha1").getAsString();
    }

    JsonObject getJson(URI fileURI) throws IOException {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(fileURI)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + this.token)
                .GET()
                .build();
        try {
            final HttpResponse<String> response = this.getClient().send(request, HttpResponse.BodyHandlers.ofString());
            final JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            // Check for API errors
            if (jsonResponse.has("error")) {
                throw new IOException("API error: " + jsonResponse.get("error").getAsString());
            }
            return jsonResponse;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException();
        }
    }

    RemoteFolder listFolder(long folderId, boolean recursive) throws IOException {
        return execute(() -> this.apiClient.listFolder(folderId, recursive).execute());
    }
    
    @Override
    public void close() {
        apiClient.shutdown();
    }
}
