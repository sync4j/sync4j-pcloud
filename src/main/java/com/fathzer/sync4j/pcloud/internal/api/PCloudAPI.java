package com.fathzer.sync4j.pcloud.internal.api;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.function.LongConsumer;

import com.fathzer.sync4j.HashAlgorithm;
import com.fathzer.sync4j.helper.PathUtils;
import com.fathzer.sync4j.pcloud.Zone;
import com.fathzer.sync4j.util.ProgressInputStream;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pcloud.sdk.ApiClient;
import com.pcloud.sdk.ApiError;
import com.pcloud.sdk.Authenticator;
import com.pcloud.sdk.PCloudSdk;
import com.pcloud.sdk.RemoteEntry;
import com.pcloud.sdk.RemoteFile;
import com.pcloud.sdk.RemoteFolder;
import com.pcloud.sdk.internal.JsonUtils;

import jakarta.annotation.Nonnull;

import com.pcloud.sdk.Authenticators;
import com.pcloud.sdk.Call;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

/**
 * Implementation of the PCloud interface using the pCloud SDK.
 * <br>
 * Note: IOException encapsulate APIError that are described in the pCloud API documentation (https://docs.pcloud.com/errors/index.html)
 */
public class PCloudAPI implements PCloud {
    private final ApiClient sdk;
    private final URI apiURI;
    private final String token;
    private final OkHttpClient httpClient;

    /**
     * Creates a new PCloudAPI instance.
     *
     * @param zone The zone to use for the API client
     * @param accessToken The access token to use for authentication
     * @throws IOException if an I/O error occurs or the authentication fails
     */
    public PCloudAPI(@Nonnull Zone zone, @Nonnull String accessToken) throws IOException {
        this.httpClient = new OkHttpClient();
        this.apiURI = zone.getRootURI();
        this.token = Objects.requireNonNull(accessToken);
        this.sdk = getApiClient(zone, accessToken, this.httpClient);
    }

    private static ApiClient getApiClient(Zone zone, String accessToken, OkHttpClient httpClient) throws IOException {
        final Authenticator authenticator = Authenticators.newOAuthAuthenticator(accessToken);
        final ApiClient client = PCloudSdk.newClientBuilder()
                .withClient(httpClient)
                .authenticator(authenticator)
                .apiHost(zone.getRootURI().getHost())
                .create();
        try {
            execute(() -> client.getUserInfo().execute());
            return client;
        } catch (AuthenticationException e) {
            client.shutdown();
            throw e;
        }
    }

    // Just for tests
    PCloudAPI(ApiClient pCloudSdk, URI apiURI, String token, OkHttpClient httpClient) {
        this.sdk = pCloudSdk;
        this.apiURI = apiURI;
        this.token = token;
        this.httpClient = httpClient;
    }

    private OkHttpClient getClient() {
        return this.httpClient;
    }

    @FunctionalInterface
    private interface PcloudCall<T> {
        T call() throws IOException, ApiError;
    }
    
    private static <T> T execute(PcloudCall<T> call) throws IOException {
        try {
            return call.call();
        } catch (ApiError e) {
//            System.out.println("API Error: " + e); //TODO remove
            int errorCode = e.errorCode();
            if (errorCode == 2055 || errorCode == 2002) {
                throw new FileNotFoundException(e.errorMessage());
            } else if (errorCode == 2094) {
                throw new AuthenticationException(e.errorMessage());
            }
            throw new IOException(e);
        }
    }

    @Override
    public RemoteEntry get(String path) throws IOException {
        return execute(() -> this.getRemoteEntry(path));
    }

    private RemoteEntry getRemoteEntry(String path) throws IOException, ApiError {
        if (PathUtils.isRoot(path)) {
            return this.sdk.loadFolder(0).execute();
        }
        RemoteFile entry = this.sdk.loadFile(path).execute();
        return entry.isFolder() ? this.sdk.loadFolder(path).execute() : entry;
    }

    @Override
    public String getHash(RemoteFile remoteFile, HashAlgorithm hashAlgorithm) throws IOException {
        Objects.requireNonNull(hashAlgorithm);
        if (hashAlgorithm != HashAlgorithm.SHA1) {
            throw new UnsupportedOperationException("Unsupported hash algorithm: " + hashAlgorithm);
        }
        return execute(() -> sdk.getChecksums(remoteFile.fileId()).execute()).getSha1().hex();
    }

    private Builder builder(URI uri) throws IOException {
        return new Request.Builder()
            .url(uri.toURL())
            .header("Accept", "application/json")
            .header("Authorization", "Bearer " + this.token);
    }

    private JsonObject getJson(Request request) throws IOException {
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

    @Override
    public InputStream getInputStream(RemoteFile remoteFile) throws IOException {
        return execute(() -> sdk.download(remoteFile).execute().inputStream());
    }

    @Override
    public RemoteFolder listFolder(long folderId, boolean recursive) throws IOException {
        return execute(() -> this.sdk.listFolder(folderId, recursive).execute());
    }

    @Override
    public RemoteFile upload(long folderId, String fileName, InputStream content, long size, long mtime, long ctime, LongConsumer progressListener) throws IOException {
        final MultipartBody.Builder builder = new MultipartBody.Builder()
            .setType(MultipartBody.FORM);

        if (folderId != 0) {
            builder.addFormDataPart("folderid", String.valueOf(folderId));
        }
        
        builder.addFormDataPart("filename", fileName);
        builder.addFormDataPart("nopartial", "true");
        builder.addFormDataPart("mtime", String.valueOf(mtime/1000));
        builder.addFormDataPart("ctime", String.valueOf(ctime/1000));
        
        try (InputStream data = progressListener == null ? content : new ProgressInputStream(content, progressListener)) {
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
            return JsonUtils.buildFile(getJson(request), this.sdk);
        }
    }

    @Override
    public RemoteFolder mkdir(long folderId, String folderName) throws IOException {
        return execute(() -> this.sdk.createFolder(folderId, folderName).execute());
    }

    @Override
    public void delete(RemoteEntry remoteEntry) throws IOException {
        Call<Boolean> delete = remoteEntry.isFolder() ? this.sdk.deleteFolder(remoteEntry.asFolder(), true) : this.sdk.delete(remoteEntry.asFile());
        execute(delete::execute);
    }

    @Override
    public void close() {
        this.sdk.shutdown();
        if (this.httpClient != null) {
            this.httpClient.dispatcher().executorService().shutdown();
            this.httpClient.connectionPool().evictAll();
        }
    }
}
