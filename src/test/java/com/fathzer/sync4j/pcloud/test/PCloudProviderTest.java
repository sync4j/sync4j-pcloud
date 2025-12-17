package com.fathzer.sync4j.pcloud.test;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.pcloud.PCloudProvider;
import com.fathzer.sync4j.pcloud.Zone;

@EnabledIfSystemProperty(named = "pcloud.token", matches = ".+")
class PCloudProviderTest {

    @Test
    void test() throws IOException {
        try (PCloudProvider provider = new PCloudProvider(getZone(), System.getProperty("pcloud.token"), "")) {
            System.out.println(provider.get(FileProvider.ROOT_PATH).asFolder().list());
        }
    }

    private static Zone getZone() {
        return Zone.valueOf(System.getProperty("pcloud.zone", "US").toUpperCase());
    }
}
