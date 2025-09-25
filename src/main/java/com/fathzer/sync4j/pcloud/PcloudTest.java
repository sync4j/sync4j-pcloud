package com.fathzer.sync4j.pcloud;

import static com.fathzer.sync4j.HashAlgorithm.SHA1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.file.LocalProvider;

public class PcloudTest {
    private static final String INDENT = "    ";
    private static final boolean WITH_HASH = false;
    
    public static void main(String[] args) throws Exception {
        final String accessToken = args[0];
        final String path = "/PhotosJM/2002";

        try (PCloudProvider provider = new PCloudProvider(accessToken)) {
            try (LocalProvider localProvider = new LocalProvider()) {
                // Copy from remote to local
                Folder localFolder = localProvider.get("C:/Users/jeanm/test").asFolder();
                File remoteFile = provider.get("/testFuse.sh").asFile();
//                copyFile(remoteFile, localFolder);

                // Copy from local to remote
                Folder remoteFolder = provider.get("/test2").asFolder();
                File localFile = localProvider.get("C:/Users/jeanm/test/linked.txt").asFile();
                copyFile(localFile, remoteFolder);

                // Read again remote to local
                remoteFile = provider.get("/test2/linked.txt").asFile();
                localFolder = localProvider.get("C:/Users/jeanm/test/reload").asFolder();
                copyFile(remoteFile, localFolder);
            }

        //     Entry entry = provider.get("/testFuse.sh", false);
        //     System.out.println(entry.getName()+" "+(entry.exists() ? "exists" : "not exists"));
        //     if (entry.isFile()) {
        //         read(entry.asFile());
        //     } else {
        //         System.out.println("Not a file");
        //     }

            printTree(provider, path);

        //     Path localPath = Paths.get("/home/jma/tmp/photosTest/2002/Pict200205010005.jpg");
        //     System.out.println("SHA1 Hash of file: " + SHA1.computeHash(localPath));

        //     File remoteFile = provider.get("/PhotosJM/2002/Pict200205010005.jpg", false).asFile();
        //     System.out.println("SHA1 Hash of remote file: " + remoteFile.getHash(SHA1));

        //     Entry remoteFile2 = provider.get("/testFuse.sh", false);
        //     System.out.println(remoteFile2.getName()+" "+(remoteFile2.exists()?"exists":"not exists"));
        // }
        // try (LocalProvider provider = new LocalProvider()) {
        //     printTree(provider, "/home/jma/tmp/photosTest/2002");
        }
    }

    private static void copyFile(File sourceFile, Folder targetFolder) throws IOException {
        final long size = sourceFile.getSize();
        final AtomicLong progress = new AtomicLong();
        targetFolder.copy(sourceFile.getName(), sourceFile, x -> {
                System.out.println("Copied " + (x - progress.getAndSet(x)) + "B");
            });
        System.out.println("Copied " + sourceFile.getName() + " to " + targetFolder.getName() + " (" + progress.get() +"/"+size + "B)");
    }

    private static void read(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
        try {
            System.out.println("-------------------------");
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("-------------------------");
        } finally {
            reader.close();
        }
    }

    private static void printTree(FileProvider provider, String rootPath) throws IOException {
        System.out.println("======================================");
        System.out.println("Folder Structure for: " + provider.getClass().getSimpleName()+":"+rootPath);
        System.out.println("--------------------------------------");
        long startTime = System.currentTimeMillis();
        Folder rootFolder = provider.get(rootPath).asFolder();
        long size = printFolderTree(rootFolder, "");
        System.out.println("Size: " + size+ " in " + (System.currentTimeMillis() - startTime) + "ms");

        System.out.println("--------------------------------------");
        System.out.println("Folder Structure (fast-list): ");
        startTime = System.currentTimeMillis();
        rootFolder = provider.get(rootPath).asFolder();
        rootFolder.preload();
        size = printFolderTree(rootFolder, "");
        System.out.println("Size: " + size+ " in " + (System.currentTimeMillis() - startTime) + "ms");
        System.out.println("======================================");
    }
    
    private static long printFolderTree(Folder folder, String indent) throws IOException {
        long size = 0;
        for (Entry entry : folder.list()) {
//            System.out.print(indent + "|");
            size ++;
            if (entry.isFile()) {
                String hash = WITH_HASH ? " (" + entry.asFile().getHash(SHA1) + ")" : "";
//                System.out.println("--- " + entry.getName() + " (" + entry.getSize() + "B - " + entry.getLastModified() + " - Hash: " + hash + ")");
            } else {
//                System.out.println("+-- " + entry.getName() + "/");
                size += printFolderTree(entry.asFolder(), indent + INDENT);
            }
        }
        return size;
    }
}
