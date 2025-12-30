package com.fathzer.sync4j.pcloud.internal.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.pcloud.PCloudProvider;
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
            "/toto/toto.txt", 2002
        );
        
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

        try (PCloudAPI api = new PCloudAPI(mockApiClient, "token")) {
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
}
