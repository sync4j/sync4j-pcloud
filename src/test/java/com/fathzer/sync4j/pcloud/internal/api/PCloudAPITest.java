package com.fathzer.sync4j.pcloud.internal.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.file.FileAlreadyExistsException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.RetryableIOException;
import com.fathzer.sync4j.pcloud.PCloudProvider;
import com.fathzer.sync4j.pcloud.Zone;
import com.pcloud.sdk.ApiClient;
import com.pcloud.sdk.ApiError;
import com.pcloud.sdk.Call;
import com.pcloud.sdk.RemoteEntry;
import com.pcloud.sdk.RemoteFile;
import com.pcloud.sdk.RemoteFolder;

class PCloudAPITest {
    @Test
    @SuppressWarnings("unchecked")
    void testGet() throws IOException, ApiError {
        ApiClient mockApiClient = mock(ApiClient.class);
        Call<RemoteFile> mockFileCall = mock(Call.class);

        // Map of paths to error codes
        Map<String, Integer> pathToErrorCode = Map.of(
                "/titi.txt", 2055,
                "/toto", 2002,
                "/toto/titi.txt", 2055,
                "/toto/toto.txt", 2002);

        // Setup the mock to throw appropriate errors based on the path
        when(mockApiClient.loadFile(anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0);
            Integer errorCode = pathToErrorCode.get(path);
            when(mockFileCall.execute()).thenThrow(new ApiError(errorCode, "File not found: " + path));
            return mockFileCall;
        });

        // Setup mock for root folder
        Call<RemoteFolder> mockFolderCall = mock(Call.class);
        RemoteFolder mockRootFolder = mock(RemoteFolder.class);
        when(mockApiClient.loadFolder(0L)).thenReturn(mockFolderCall);
        when(mockFolderCall.execute()).thenReturn(mockRootFolder);

        try (PCloudAPI api = new PCloudAPI(mockApiClient, null, null, null)) {
            // Test root folder
            RemoteEntry root = api.get(PCloudProvider.ROOT_PATH);
            assertSame(mockRootFolder, root);

            // Test error cases
            assertThrows(FileNotFoundException.class, () -> api.get("/titi.txt"));
            assertThrows(FileNotFoundException.class, () -> api.get("/toto"));
            assertThrows(FileNotFoundException.class, () -> api.get("/toto/titi.txt"));
            assertThrows(FileNotFoundException.class, () -> api.get("/toto/toto.txt"));

            assertThrows(NullPointerException.class, () -> api.get(null));
        }
    }
    
    @Test
    void testConstructor() {
        assertThrows(NullPointerException.class, () -> new PCloudAPI(null, "token"));
        assertThrows(NullPointerException.class, () -> new PCloudAPI(Zone.US, null));
    }

    @Test
    void testErrorClassification() {
        // Transient errors
        for (int code : new int[] {1900, 1999, 4000, 4001, 5000, 5001}) {
            assertInstanceOf(RetryableIOException.class, PCloudAPI.toIOException(code, "msg", null, null), "Error " + code + " should be retryable");
        }
        // Not found
        for (int code : new int[] {2002, 2005, 2055}) {
            assertInstanceOf(FileNotFoundException.class, PCloudAPI.toIOException(code, "msg", null, null), "Error " + code + " should be a not found error");
        }
        assertInstanceOf(FileAlreadyExistsException.class, PCloudAPI.toIOException(2004, "msg", null, null));
        assertInstanceOf(AuthenticationException.class, PCloudAPI.toIOException(2094, "msg", null, null));
        // Definitive errors
        for (int code : new int[] {1000, 1001, 2003, 2008, 3000, 6000, 7001}) {
            IOException e = PCloudAPI.toIOException(code, "msg", new ApiError(code, "msg"), null);
            assertFalse(e instanceof RetryableIOException, "Error " + code + " should be definitive");
            assertEquals(IOException.class, e.getClass());
            assertInstanceOf(ApiError.class, e.getCause());
        }
        // The cause is kept
        ApiError cause = new ApiError(5000, "internal");
        assertSame(cause, PCloudAPI.toIOException(5000, "internal", cause, null).getCause());
        assertSame(cause, PCloudAPI.toIOException(2004, "exists", cause, null).getCause());
    }

    @Test
    void testRetryAfter() {
        assertEquals(Duration.ofSeconds(7), ((RetryableIOException) PCloudAPI.toIOException(4000, "msg", null, Duration.ofSeconds(7))).getRetryAfter());
        assertEquals(Duration.ofSeconds(12), PCloudAPI.parseRetryAfter(" 12 "));
        assertNull(PCloudAPI.parseRetryAfter(null));
        assertNull(PCloudAPI.parseRetryAfter("Wed, 21 Oct 2015 07:28:00 GMT"));
        assertNull(PCloudAPI.parseRetryAfter("-3"));
    }

    @Test
    void testHttpStatus() {
        for (int status : new int[] {429, 500, 502, 503, 504, 509}) {
            IOException e = PCloudAPI.toIOException(status, "body", "5");
            assertInstanceOf(RetryableIOException.class, e, "Status " + status + " should be retryable");
            assertEquals(Duration.ofSeconds(5), ((RetryableIOException) e).getRetryAfter());
        }
        for (int status : new int[] {400, 401, 403, 404}) {
            assertEquals(IOException.class, PCloudAPI.toIOException(status, "body", null).getClass(), "Status " + status + " should be definitive");
        }
        assertNull(((RetryableIOException) PCloudAPI.toIOException(429, "body", null)).getRetryAfter());
    }

    @SuppressWarnings("unchecked")
    private static PCloudAPI apiFailingWith(Exception failure) throws Exception {
        ApiClient client = mock(ApiClient.class);
        Call<RemoteFolder> call = mock(Call.class);
        when(client.createFolder(anyLong(), anyString())).thenReturn(call);
        when(call.execute()).thenThrow(failure);
        return new PCloudAPI(client, null, null, null);
    }

    @Test
    void testNetworkErrorsAreRetryable() throws Exception {
        try (PCloudAPI api = apiFailingWith(new SocketTimeoutException("timeout"))) {
            assertInstanceOf(RetryableIOException.class, assertThrows(IOException.class, () -> api.mkdir(1, "folder")));
        }
        try (PCloudAPI api = apiFailingWith(new ConnectException("refused"))) {
            assertInstanceOf(RetryableIOException.class, assertThrows(IOException.class, () -> api.mkdir(1, "folder")));
        }
        // Other I/O errors are not
        try (PCloudAPI api = apiFailingWith(new IOException("other"))) {
            assertEquals(IOException.class, assertThrows(IOException.class, () -> api.mkdir(1, "folder")).getClass());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testMkdirIsIdempotent() throws Exception {
        ApiClient client = mock(ApiClient.class);
        Call<RemoteFolder> createCall = mock(Call.class);
        when(client.createFolder(1L, "folder")).thenReturn(createCall);
        when(client.createFolder(1L, "file")).thenReturn(createCall);
        when(client.createFolder(1L, "ghost")).thenReturn(createCall);
        when(createCall.execute()).thenThrow(new ApiError(2004, "File or folder already exists"));

        RemoteEntry existingFolder = mock(RemoteEntry.class);
        RemoteFolder existing = mock(RemoteFolder.class);
        when(existingFolder.name()).thenReturn("folder");
        when(existingFolder.isFolder()).thenReturn(true);
        when(existingFolder.asFolder()).thenReturn(existing);
        RemoteEntry existingFile = mock(RemoteEntry.class);
        when(existingFile.name()).thenReturn("file");
        when(existingFile.isFolder()).thenReturn(false);
        RemoteFolder parent = mock(RemoteFolder.class);
        when(parent.children()).thenReturn(List.of(existingFolder, existingFile));
        Call<RemoteFolder> listCall = mock(Call.class);
        when(client.listFolder(1L, false)).thenReturn(listCall);
        when(listCall.execute()).thenReturn(parent);

        try (PCloudAPI api = new PCloudAPI(client, null, null, null)) {
            // An existing folder is returned
            assertSame(existing, api.mkdir(1, "folder"));
            // A file with the same name is an error
            FileAlreadyExistsException e = assertThrows(FileAlreadyExistsException.class, () -> api.mkdir(1, "file"));
            assertInstanceOf(IOException.class, e.getCause());
            // If the entry that blocks the creation can't be found, the error is reported
            assertThrows(FileAlreadyExistsException.class, () -> api.mkdir(1, "ghost"));
        }
    }
}
