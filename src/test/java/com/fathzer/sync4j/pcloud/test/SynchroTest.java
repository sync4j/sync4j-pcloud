package com.fathzer.sync4j.pcloud.test;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.HashAlgorithm;
import com.fathzer.sync4j.file.LocalProvider;
import com.fathzer.sync4j.pcloud.PCloudProvider;
import com.fathzer.sync4j.pcloud.Zone;
import com.fathzer.sync4j.sync.Event;
import com.fathzer.sync4j.sync.Synchronization;
import com.fathzer.sync4j.sync.Event.CopyFileAction;
import com.fathzer.sync4j.sync.parameters.SyncParameters;
import com.fathzer.sync4j.sync.parameters.FileComparator;

public class SynchroTest {
    public static void main(String[] args) throws Exception {
        try (Watcher watcher = new Watcher()) {
            SyncParameters params = new SyncParameters()
//                .dryRun(true)
                .fileComparator(FileComparator.of(List.of(FileComparator.SIZE, FileComparator.MOD_DATE, FileComparator.hash(HashAlgorithm.SHA1))))
                .eventListener(watcher)
                // .errorManager((e, action) -> {
                //    System.out.println("Error: on " + action + " with " + e.getMessage());
                //    return false;
                // })
            ;
            params.performance()
//                .fastList(true)
                .maxWalkThreads(4)
                .maxCopyThreads(5)
                .maxComparisonThreads(8)
            ;

            try (FileProvider local = new LocalProvider(Paths.get("/home/jma/tmp")); FileProvider pCloud = new PCloudProvider(Zone.US, System.getenv("PCLOUD_TOKEN"), "")) {
                Folder source = pCloud.get("/PhotosJM/2002").asFolder();
                Folder target = local.get("/PhotosJM/2002").asFolder();
                try (Synchronization synchronizer = new Synchronization(source, target, params)) {
                    watcher.setSynchronizer(synchronizer);
                    final long start = System.currentTimeMillis();
                    synchronizer.start();
                    synchronizer.waitFor();
                    final long end = System.currentTimeMillis();
                    System.out.println("All tasks finished at " + end + " in " + (end - start) + " ms");
                    System.out.println("Final Stats: " + synchronizer.getStatistics());
                    System.out.println(watcher.getCounters());
                }
            }
        }
        System.out.println("Bye at " + System.currentTimeMillis());
    }

    private static class Watcher implements Consumer<Event>, AutoCloseable {
        private final AtomicLong planned = new AtomicLong();
        private final AtomicLong running = new AtomicLong();
        private final AtomicLong completed = new AtomicLong();
        private final AtomicLong failed = new AtomicLong();

        private final ScheduledExecutorService timer;
        private Synchronization synchronizer;

        private Watcher() {
            this.timer = Executors.newScheduledThreadPool(1);
            timer.scheduleAtFixedRate(() -> {
                if (synchronizer != null) {
                    System.out.println(synchronizer.getStatistics());
                }
            }, 0, 1, TimeUnit.SECONDS);
        }
        
        public void setSynchronizer(Synchronization synchronizer) {
            this.synchronizer = synchronizer;
        }

        public String getCounters() {
            return "Planned: " + planned.get()+", running: " + running.get()+", completed: " + completed.get()+", failed: " + failed.get();
        }
        
        @Override
        public void accept(Event event) {
            switch (event.status()) {
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
            if (event.action() instanceof CopyFileAction) {
                System.out.println(event.action()+" "+event.status());
            }
        }

        @Override
        public void close() {
            timer.shutdownNow();
        }
    }
}
