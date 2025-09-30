package com.fathzer.sync4j.pcloud.test;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.HashAlgorithm;
import com.fathzer.sync4j.file.LocalProvider;
import com.fathzer.sync4j.pcloud.PCloudProvider;
import com.fathzer.sync4j.pcloud.Zone;
import com.fathzer.sync4j.sync.Event;
import com.fathzer.sync4j.sync.Synchronizer;
import com.fathzer.sync4j.sync.parameters.SyncParameters;
import com.fathzer.sync4j.sync.parameters.FileComparator;

public class SynchroTest {
    public static void main(String[] args) throws Exception {
        try (Watcher watcher = new Watcher()) {
            SyncParameters params = new SyncParameters();
            params.dryRun(true).
                fileComparator(FileComparator.of(List.of(FileComparator.SIZE, FileComparator.MOD_DATE, FileComparator.hash(HashAlgorithm.SHA1)))).
                eventListener(watcher).
                performance().fastList(true).
                    maxCopyThreads(5).
                    maxComparisonThreads(8);

            try (PCloudProvider provider = new PCloudProvider(Zone.US, System.getenv("PCLOUD_TOKEN"))) {
                Folder source = provider.get("/PhotosJM/2002").asFolder();
                Folder target = LocalProvider.INSTANCE.get("/home/jma/tmp/photosTest/2002").asFolder();
                try (Synchronizer synchronizer = new Synchronizer(source, target, params)) {
                    synchronizer.start();
                    System.out.println("Waiting for tasks to finish");
                    synchronizer.waitFor();
                    System.out.println("All tasks finished");
                    System.out.println(synchronizer.getStatistics());
                }
            }
        }
    }

    private static class Watcher implements Consumer<Event>, AutoCloseable {
        private final AtomicLong planned = new AtomicLong();
        private final AtomicLong running = new AtomicLong();
        private final AtomicLong completed = new AtomicLong();
        private final AtomicLong failed = new AtomicLong();

        private final ScheduledExecutorService timer;

        private Watcher() {
            this.timer = Executors.newScheduledThreadPool(1);
            timer.scheduleAtFixedRate(() -> {
                System.out.println("Planned: " + planned.get()+", running: " + running.get()+", completed: " + completed.get()+", failed: " + failed.get());
            }, 0, 1, TimeUnit.SECONDS);
        }
        
        @Override
        public void accept(Event event) {
            switch (event.getStatus()) {
                case PLANNED:
                    planned.incrementAndGet();
                    break;
                case STARTED:
                    running.incrementAndGet();
                    break;
                case COMPLETED:
                    completed.incrementAndGet();
                    running.decrementAndGet();
                    break;
                case FAILED:
                    failed.incrementAndGet();
                    running.decrementAndGet();
                    break;
            }
//            System.out.println(event.getAction()+" "+event.getStatus());
        }

        @Override
        public void close() {
            timer.shutdown();
        }
    }
}
