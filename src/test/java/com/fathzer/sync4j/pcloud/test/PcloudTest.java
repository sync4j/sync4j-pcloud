package com.fathzer.sync4j.pcloud.test;

import static com.fathzer.sync4j.HashAlgorithm.SHA1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.File;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.file.LocalProvider;
import com.fathzer.sync4j.pcloud.PCloudProvider;
import com.fathzer.sync4j.pcloud.Zone;

public class PcloudTest {
    private static final String INDENT = "    ";
    private static final boolean WITH_HASH = false;
    
    public static void main(String[] args) throws Exception {
        final String accessToken = args[0];
        final String path = "/PhotosJM/2002";

        try (FileProvider provider = new PCloudProvider(Zone.US, accessToken)) {
            try (FileProvider localProvider = new LocalProvider()) {
                // Copy from remote to local
            //    Folder localFolder = localProvider.get("/home/jma/tmp").asFolder();
            //    File remoteFile = provider.get("/testFuse.sh").asFile();
            //    copyFile(remoteFile, localFolder);

                // Copy from local to remote
                // Folder remoteFolder = provider.get("/test").asFolder();
                // File localFile = localProvider.get("/home/jma/tmp/photosTest/1998/IMGP7236.jpg").asFile();
                // File remoteFile = copyFile(localFile, remoteFolder);

                // System.out.println(remoteFile);

                // Read again remote to local
//                remoteFile = provider.get("/test2/linked.txt").asFile();
//                localFolder = localProvider.get("C:/Users/jeanm/test/reload").asFolder();
//                copyFile(remoteFile, localFolder);

        //     Entry entry = provider.get("/testFuse.sh", false);
        //     System.out.println(entry.getName()+" "+(entry.exists() ? "exists" : "not exists"));
        //     if (entry.isFile()) {
        //         read(entry.asFile());
        //     } else {
        //         System.out.println("Not a file");
        //     }

        //        printTree(provider, path);

        //     Path localPath = Paths.get("/home/jma/tmp/photosTest/2002/Pict200205010005.jpg");
        //     System.out.println("SHA1 Hash of file: " + SHA1.computeHash(localPath));

        //     File remoteFile = provider.get("/PhotosJM/2002/Pict200205010005.jpg", false).asFile();
        //     System.out.println("SHA1 Hash of remote file: " + remoteFile.getHash(SHA1));

        //     Entry remoteFile2 = provider.get("/testFuse.sh", false);
        //     System.out.println(remoteFile2.getName()+" "+(remoteFile2.exists()?"exists":"not exists"));
        //        printTree(localProvider, "/home/jma/tmp/photosTest/2002");

                // Folder newFolder = provider.get("/PhotosJM").asFolder().mkdir("new Folder");
                // System.out.println("New folder created: " + newFolder.getName());

                Entry remoteEntry = provider.get("/PhotosJM/2002/test1/test2/toto.xml");
                System.out.println(getFullPath(remoteEntry));
            }
        }
    }

    private static String getFullPath(Entry entry) throws IOException {
        List<String> path = new LinkedList<>();
        do {
            path.add(entry.getName());
            System.out.println(entry.getName()+" "+entry.exists()+" "+entry.getClass().getSimpleName());
            entry = entry.getParent().orElse(null);
        } while (entry != null);
        Collections.reverse(path);
        return String.join("/", path);
    }

    private static File copyFile(File sourceFile, Folder targetFolder) throws IOException {
        final long size = sourceFile.getSize();
        final AtomicLong progress = new AtomicLong();
        File result = targetFolder.copy(sourceFile.getName(), sourceFile, x -> {
                System.out.println("Copied " + (x - progress.getAndSet(x)) + "B");
            });
        System.out.println("Copied " + sourceFile.getName() + " to " + targetFolder.getName() + " (" + progress.get() +"/"+size + "B)");
        return result;
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

        if (provider.isFastListSupported()) {
            System.out.println("--------------------------------------");
            System.out.println("Folder Structure (fast-list): ");
            startTime = System.currentTimeMillis();
            rootFolder = provider.get(rootPath).asFolder();
            rootFolder.preload();
            size = printFolderTree(rootFolder, "");
            System.out.println("Size: " + size+ " in " + (System.currentTimeMillis() - startTime) + "ms");
            System.out.println("======================================");
        }
    }
    
    private static long printFolderTree(Folder folder, String indent) throws IOException {
        long size = 0;
        for (Entry entry : folder.list()) {
//            System.out.print(indent + "|");
            size ++;
            if (entry.isFile()) {
                File file = entry.asFile();
                String hash = WITH_HASH ? " (" + file.getHash(SHA1) + ")" : "";
//                System.out.println("--- " + file.getName() + " (" + file.getSize() + "B - " + file.getLastModified() + " - Hash: " + hash + ")");
            } else {
                System.out.println("+-- " + entry.getName() + "/");
                size += printFolderTree(entry.asFolder(), indent + INDENT);
            }
        }
        return size;
    }
}
