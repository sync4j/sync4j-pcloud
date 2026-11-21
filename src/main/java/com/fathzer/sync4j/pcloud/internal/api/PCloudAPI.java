package com.fathzer.sync4j.pcloud.internal.api;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongConsumer;

import com.fathzer.sync4j.HashAlgorithm;
import com.fathzer.sync4j.RetryableIOException;
import com.fathzer.sync4j.helper.PathUtils;
import com.fathzer.sync4j.pcloud.Zone;
import com.fathzer.sync4j.util.ProgressInputStream;
import com.google.gson.JsonElement;
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
 * <br>Errors are reported as described in the {@link com.fathzer.sync4j.FileProvider} contract:
 * <ul>
 * <li>{@link RetryableIOException} for transient errors: rate limiting (4xxx), server side errors (5xxx), "safe to retry" errors (19xx),
 * HTTP 429, 500, 502, 503, 504 and 509 statuses, timeouts and network errors.</li>
 * <li>{@link FileNotFoundException} if an entry does not exist.</li>
 * <li>{@link FileAlreadyExistsException} if a folder can't be created because a file with the same name exists.</li>
 * <li>{@link AuthenticationException} if the token is rejected.</li>
 * <li>A plain {@link IOException} for any other (definitive) error.</li>
 * </ul>
 * This class never retries by itself.
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
    
    /**
     * Gets the underlying pCloud SDK instance.
     * @return the pCloud SDK instance
     */
    @Nonnull
    public ApiClient getSdk() {
    	return sdk;
    }

    @FunctionalInterface
    private interface PcloudCall<T> {
        T call() throws IOException, ApiError;
    }
    
    private static <T> T execute(PcloudCall<T> call) throws IOException {
        try {
            return call.call();
        } catch (ApiError e) {
            throw toIOException(e.errorCode(), e.errorMessage(), e, null);
        } catch (SocketException | SocketTimeoutException e) {
            // Network glitch (connection reset or refused, timeout, ...)
            throw new RetryableIOException(e.toString(), e);
        }
    }

    /**
     * Converts a pCloud error to the exception expected by the {@link com.fathzer.sync4j.FileProvider} contract.
     * <br>Errors are classified by group (see https://docs.pcloud.com/errors/index.html).
     * @param errorCode the pCloud error code
     * @param message the error message
     * @param cause the cause, can be null
     * @param retryAfter the delay requested by the server, null if none
     * @return the exception to throw
     */
    static IOException toIOException(int errorCode, String message, Throwable cause, Duration retryAfter) {
        final int group = errorCode / 1000;
        final IOException result;
        if (group == 4 || group == 5 || errorCode / 100 == 19) {
            // 4xxx: rate limiting, 5xxx: server side errors, 19xx: "safe to retry the request later"
            result = new RetryableIOException(message, cause, retryAfter);
        } else if (errorCode == 2055 || errorCode == 2002 || errorCode == 2005) {
            result = new FileNotFoundException(message);
        } else if (errorCode == 2004) {
            result = new FileAlreadyExistsException(null, null, message);
        } else if (errorCode == 2094) {
            result = new AuthenticationException(message);
        } else {
            // Any other error is definitive
            return cause == null ? new IOException(message) : new IOException(cause);
        }
        if (result.getCause() == null && cause != null) {
            result.initCause(cause);
        }
        return result;
    }

    private static final String ERROR_FIELD = "error";
    private static final Set<Integer> RETRYABLE_HTTP_STATUS = Set.of(429, 500, 502, 503, 504, 509);

    /**
     * Converts an unexpected HTTP response to an exception.
     * @param status the HTTP status
     * @param body the body of the response
     * @param retryAfterHeader the value of the Retry-After header, null if the response has no such header
     * @return the exception to throw
     */
    static IOException toIOException(int status, String body, String retryAfterHeader) {
        final String message = "Unexpected response " + status + ": " + body;
        if (RETRYABLE_HTTP_STATUS.contains(status)) {
            return new RetryableIOException(message, null, parseRetryAfter(retryAfterHeader));
        }
        return new IOException(message);
    }

    /**
     * Parses the value of a Retry-After header.
     * @param value the value of the header
     * @return the delay, or null if the value is missing or not a number of seconds (the date format is ignored)
     */
    static Duration parseRetryAfter(String value) {
        if (value == null) {
            return null;
        }
        try {
            final long seconds = Long.parseLong(value.trim());
            return seconds < 0 ? null : Duration.ofSeconds(seconds);
        } catch (NumberFormatException e) {
            return null;
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
        try (Response response = this.httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw toIOException(response.code(), response.body().string(), response.header("Retry-After"));
            }
            final JsonObject jsonResponse = JsonParser.parseString(response.body().string()).getAsJsonObject();
            // Check for API errors
            final int result = jsonResponse.has("result") ? jsonResponse.get("result").getAsInt() : 0;
            final JsonElement error = jsonResponse.get(ERROR_FIELD);
            if (result != 0 || error != null) {
                final String message = "API error: " + (error != null ? error.getAsString() : result);
                throw toIOException(result, message, null, parseRetryAfter(response.header("Retry-After")));
            }
            return jsonResponse;
        } catch (SocketException | SocketTimeoutException e) {
            throw new RetryableIOException(e.toString(), e);
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
        try {
            return execute(() -> this.sdk.createFolder(folderId, folderName).execute());
        } catch (FileAlreadyExistsException e) {
            // mkdir is idempotent: an existing folder is returned, but a file with the same name is an error
            for (RemoteEntry child : listFolder(folderId, false).children()) {
                if (child.name().equals(folderName)) {
                    if (child.isFolder()) {
                        return child.asFolder();
                    }
                    throw (FileAlreadyExistsException) new FileAlreadyExistsException(folderName, null, "A file with the same name exists").initCause(e);
                }
            }
            // The entry that blocked the creation is not visible (yet?)
            throw e;
        }
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
