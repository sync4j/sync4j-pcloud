package com.fathzer.sync4j.pcloud.test;

import java.io.IOException;

import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.file.LocalProvider;
import com.fathzer.sync4j.pcloud.PCloudProvider;
import com.fathzer.sync4j.pcloud.Zone;
import com.fathzer.sync4j.sync.Synchronizer;
import com.fathzer.sync4j.sync.parameters.SyncParameters;

public class SynchroTest {
    public static void main(String[] args) throws IOException {
        SyncParameters params = new SyncParameters();
        params.performance().fastList(true).
            maxCopyThreads(8).
            maxComparisonThreads(5);
        params.dryRun(true);

        try (PCloudProvider provider = new PCloudProvider(Zone.US, System.getenv("PCLOUD_TOKEN"))) {
            Folder source = provider.get("/PhotosJM/2002").asFolder();
            Folder target = LocalProvider.INSTANCE.get("/home/jma/tmp/photosTest/2002").asFolder();
            Synchronizer synchronizer = new Synchronizer(source, target, params);
            synchronizer.run();
        }
    }
}
