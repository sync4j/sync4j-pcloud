package com.fathzer.sync4j.pcloud;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.HashAlgorithm;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pcloud.sdk.ApiClient;
import com.pcloud.sdk.Authenticators;
import com.pcloud.sdk.PCloudSdk;

public class PCloudProvider implements FileProvider<PcloudFile> {
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
    public String getHash(PcloudFile file, HashAlgorithm hashAlgorithm) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (hashAlgorithm == null) {
            throw new IllegalArgumentException("Hash algorithm cannot be null");
        }
        if (hashAlgorithm != HashAlgorithm.SHA1) {
            throw new IllegalArgumentException("Unsupported hash algorithm: " + hashAlgorithm);
        }
        final URI fileURI = this.apiURI.resolve("checksumfile?fileid=" + file.getRemoteFile().fileId());
        return getJson(fileURI).get("sha1").getAsString();
    }

    JsonObject getJson(URI fileURI) throws IOException {
        final HttpClient client = this.getClient();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(fileURI)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + this.token)
                .GET()
                .build();
        try {
            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
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
    
    @Override
    public void close() {
        apiClient.shutdown();
    }
}
