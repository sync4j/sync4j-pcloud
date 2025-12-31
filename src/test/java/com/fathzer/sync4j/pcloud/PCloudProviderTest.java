package com.fathzer.sync4j.pcloud;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.Folder;

import com.fathzer.sync4j.pcloud.internal.api.PCloud;
import com.fathzer.sync4j.pcloud.internal.api.PCloudAPI;
import com.fathzer.sync4j.test.AbstractFileProviderTest;
import com.fathzer.sync4j.test.UnderlyingFileSystem;

import com.pcloud.sdk.RemoteFolder;

@EnabledIfSystemProperty(named = "pcloud.token", matches = ".+")
class PCloudProviderTest extends AbstractFileProviderTest {
    private static final String TEST_FOLDER_PREFIX = "sync4j-test-";

    private static PCloud pcloud;
    private static boolean hasCleanupFailure;

    private RemoteFolder testFolder;

    @AfterAll
    static void apiCleanup() {
        if (pcloud != null) {
            pcloud.close();
        }
    }

    @Test
    void testRootAccount() throws IOException {
        try (PCloudProvider provider = new PCloudProvider(getZone(), getToken(), "")) {
            //check some root properties on root folder that is the account's root
            Folder root = provider.get(FileProvider.ROOT_PATH).asFolder();
            assertTrue(root.exists());
            assertTrue(root.isFolder());
            assertEquals("", root.getName());
            assertNull(root.getParent());
        }
    }

    @Test
    void testRootIsNotAFolder() throws IOException {
        String token = getToken();
        Zone zone = getZone();
        String rootPath = "/"+testFolder.name()+"/";
        assertThrows(IOException.class, () -> new PCloudProvider(zone, token, rootPath+"nonExistingFolder"));
        root.copy("file.txt", createMockFile("toto"), null);
        assertThrows(IOException.class, () -> new PCloudProvider(zone, token, rootPath+"file.txt"));
    }
    
    private static String getToken() {
        return System.getProperty("pcloud.token");
    }

    private static Zone getZone() {
        return Zone.valueOf(System.getProperty("pcloud.zone", "US").toUpperCase());
    }

    private PCloud getPCloud() throws IOException {
        if (pcloud == null) {
            pcloud = new PCloudAPI(getZone(), System.getProperty("pcloud.token"));
        }
        return pcloud;
    }

    @Override
    protected FileProvider createFileProvider(TestInfo testInfo) throws IOException {
        assumeFalse(hasCleanupFailure, "Previous test failed to clean up, prevent creating new test folder");
        testFolder = getPCloud().mkdir(0, TEST_FOLDER_PREFIX + System.currentTimeMillis());
        return new PCloudProvider(getZone(), System.getProperty("pcloud.token"), "/"+testFolder.name());
    }
    
    @Override
    protected void cleanUpProvider() throws IOException {
        if (testFolder == null) {
            return;
        }
        try {
            pcloud.delete(testFolder);
        } catch (IOException e) {
            hasCleanupFailure = true;
            throw e;
        }
    }

    @Override
    protected UnderlyingFileSystem getUnderlyingFileSystem() {
        return new PCloudFileSystem(getZone(), getToken(), "/" + testFolder.name());
    }
}
