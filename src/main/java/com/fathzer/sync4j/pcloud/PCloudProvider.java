package com.fathzer.sync4j.pcloud;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.LongConsumer;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.HashAlgorithm;
import com.fathzer.sync4j.util.ProgressInputStream;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pcloud.sdk.ApiClient;
import com.pcloud.sdk.ApiError;
import com.pcloud.sdk.Authenticators;
import com.pcloud.sdk.PCloudSdk;
import com.pcloud.sdk.RemoteEntry;
import com.pcloud.sdk.RemoteFile;
import com.pcloud.sdk.RemoteFolder;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

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
    public Entry get(String path) throws IOException {
        try {
            return execute(() -> {
                RemoteEntry remoteFile = this.getRemoteEntry(path);
                if (remoteFile.isFolder()) {
                    if (!path.isEmpty()) {
                        remoteFile = this.apiClient.loadFolder(path).execute();
                    }
                    return new PcloudFolder(remoteFile, this, false);
                }
                return new PcloudFile(remoteFile, this);
            });
        } catch (FileNotFoundException e) {
            return new PcloudMissingFile(path, this);
        }
    }

    private RemoteEntry getRemoteEntry(String path) throws IOException, ApiError {
        if (path.isEmpty()) {
            return this.apiClient.loadFolder(0).execute();
        }
        return this.apiClient.loadFile(path).execute();
    }

    PcloudFolder getRemoteFolder(long folderId) throws IOException {
        RemoteFolder remoteFolder = this.execute(() -> this.apiClient.loadFolder(folderId).execute());
        return new PcloudFolder(remoteFolder, this, false);
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
        return getJson(builder(fileURI).build()).get("sha1").getAsString();
    }

    void delete(RemoteEntry remoteEntry) throws IOException {
        final boolean deleted = execute(() -> apiClient.delete(remoteEntry).execute());
        if (!deleted) {
            throw new IOException("Failed to delete file: " + remoteEntry);
        }
    }

    InputStream getInputStream(RemoteFile remoteFile) throws IOException {
        return execute(() -> apiClient.download(remoteFile).execute().inputStream());
    }

    JsonObject getJson(Request request) throws IOException {
        try (Response response = this.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response " + response + ": " + response.body().string());
            }
            final JsonObject jsonResponse = JsonParser.parseString(response.body().string()).getAsJsonObject();
            // Check for API errors
            if (jsonResponse.has("error")) {
                throw new IOException("API error: " + jsonResponse.get("error").getAsString());
            }
            return jsonResponse;
        }
    }
    
    private Builder builder(URI uri) throws IOException {
    	return new Request.Builder()
                .url(uri.toURL())
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + this.token);
    }

    RemoteFolder listFolder(long folderId, boolean recursive) throws IOException {
        return execute(() -> this.apiClient.listFolder(folderId, recursive).execute());
    }

    JsonObject upload(long folderId, String fileName, File content, LongConsumer progressListener) throws IOException {
        MultipartBody.Builder builder = new MultipartBody.Builder()
        		.setType(MultipartBody.FORM);

        if (folderId != 0) {
            builder.addFormDataPart("folderid", String.valueOf(folderId));
        }
        
        builder.addFormDataPart("filename", fileName);
        builder.addFormDataPart("nopartial", "true");
        builder.addFormDataPart("mtime", String.valueOf(content.getLastModified()/1000));
        builder.addFormDataPart("ctime", String.valueOf(content.getCreationTime()/1000));
        
        final long size = content.getSize();

        try (InputStream data = new ProgressInputStream(content.getInputStream(), progressListener)) {
	        // Create RequestBody that properly handles InputStream with known length
	        RequestBody fileBody = new RequestBody() {
	            @Override
	            public MediaType contentType() {
	                return MediaType.get("application/octet-stream");
	            }
	
	            @Override
	            public long contentLength() {
	                return size;
	            }
	
	            @Override
	            public void writeTo(BufferedSink sink) throws IOException {
	                try (data) {
	                    byte[] buffer = new byte[8192];
	                    int bytesRead;
	                    while ((bytesRead = data.read(buffer)) != -1) {
	                        sink.write(buffer, 0, bytesRead);
	                    }
	                }
	            }
	        };
	
	        builder.addFormDataPart("file", fileName, fileBody);
	
	        RequestBody requestBody = builder.build();
	
	        Request request = builder(apiURI.resolve("uploadfile"))
	                .post(requestBody)
	                .build();
	
            return getJson(request);
        }
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
