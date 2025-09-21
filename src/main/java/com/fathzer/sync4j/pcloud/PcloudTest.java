package com.fathzer.sync4j.pcloud;

import static com.fathzer.sync4j.HashAlgorithm.SHA1;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fathzer.sync4j.File;
import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.file.LocalProvider;

public class PcloudTest {
    private static final String INDENT = "    ";
    private static final boolean WITH_HASH = false;
    
    public static void main(String[] args) throws Exception {
        final String accessToken = args[0];
        final String path = "/PhotosJM/2002";

        try (PCloudProvider provider = new PCloudProvider(accessToken)) {
            printTree(provider, path);

            Path localPath = Paths.get("/home/jma/tmp/photosTest/2002/Pict200205010005.jpg");
            System.out.println("SHA1 Hash of file: " + SHA1.computeHash(localPath));

            File remoteFile = provider.get("/PhotosJM/2002/Pict200205010005.jpg", false);
            System.out.println("SHA1 Hash of remote file: " + remoteFile.getHash(SHA1));

            try {
                File remoteFile2 = provider.get("/testFuse.sh", false);
                System.out.println(remoteFile2);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (LocalProvider provider = new LocalProvider()) {
            printTree(provider, "/home/jma/tmp/photosTest/2002");
        }
    }

    private static void printTree(FileProvider provider, String rootPath) throws IOException {
        File rootFolder = provider.get(rootPath, false);
        System.out.println("Remote Folder Structure for: " + rootFolder.getName());
        System.out.println("======================================");
        long startTime = System.currentTimeMillis();
        long size = printFolderTree(rootFolder, "");
        System.out.println("Size: " + size+ " in " + (System.currentTimeMillis() - startTime) + "ms");

        System.out.println("======================================");
        System.out.println("Structure with fast-list: ");
        System.out.println("======================================");
        startTime = System.currentTimeMillis();
        rootFolder = provider.get(rootPath, true);
        System.out.println("Size: " + size+ " in " + (System.currentTimeMillis() - startTime) + "ms");

        size = printFolderTree(rootFolder, "");
        System.out.println("Size: " + size);
    }
    
    private static long printFolderTree(File folder, String indent) throws IOException {
        long size = 0;
        for (File entry : folder.list()) {
//            System.out.print(indent + "|");
            size ++;
            if (entry.isFile()) {
                String hash = WITH_HASH ? " (" + entry.getHash(SHA1) + ")" : "";
//                System.out.println("--- " + entry.getName() + " (" + entry.getSize() + "B - " + entry.getLastModified() + " - Hash: " + hash + ")");
            } else {
//                System.out.println("+-- " + entry.getName() + "/");
                size += printFolderTree(entry, indent + INDENT);
            }
        }
        return size;
    }
}
