package com.fathzer.sync4j.pcloud.test;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.pcloud.PCloudProvider;
import com.fathzer.sync4j.pcloud.Zone;
import com.fathzer.sync4j.pcloud.internal.api.PCloud;
import com.fathzer.sync4j.pcloud.internal.api.PCloudAPI;
import com.fathzer.sync4j.test.AbstractFileProviderTest;
import com.pcloud.sdk.RemoteFolder;

@EnabledIfSystemProperty(named = "pcloud.token", matches = ".+")
class PCloudProviderTest extends AbstractFileProviderTest {
    private static final String TEST_FOLDER_PREFIX = "sync4j-test-";

    private static PCloud pcloud;
    private static boolean hasCleanupFailure;

    private RemoteFolder testFolder;

    @Test
    void test() throws IOException {
        try (PCloudProvider provider = new PCloudProvider(getZone(), System.getProperty("pcloud.token"), "")) {
        	//TODO check some root properties
        }
    }

    private static Zone getZone() {
        return Zone.valueOf(System.getProperty("pcloud.zone", "US").toUpperCase());
    }

    private PCloud getPCloud() throws IOException {
        //TODO close pcloud when done
        if (pcloud == null) {
            pcloud = new PCloudAPI(getZone(), System.getProperty("pcloud.token"));
        }
        return pcloud;
    }

    @Override
    protected FileProvider createFileProvider(TestInfo testInfo) throws IOException {
    	Optional<Method> method = testInfo.getTestMethod();
    	String name = method.isPresent() ? method.get().getName() : null;
//        if (!"testGetParent".equals(name) && !"testGet".equals(name)) {
//            return null;
//        }
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
        // TODO
        return null;
    }
}
