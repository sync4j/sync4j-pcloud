package com.fathzer.sync4j.pcloud;

import static com.fathzer.sync4j.HashAlgorithm.SHA1;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fathzer.sync4j.File;

public class PcloudTest {
    private static final String INDENT = "    ";
    private static final boolean WITH_HASH = false;
    
    public static void main(String[] args) throws Exception {
        final String accessToken = args[0];
        final String path = "/PhotosJM";
        try (PCloudProvider pCloudManager = new PCloudProvider(accessToken)) {

            File rootFolder = pCloudManager.get(path, false);
            System.out.println("Remote Folder Structure for: " + rootFolder.getName());
            System.out.println("======================================");
            long startTime = System.currentTimeMillis();
            long size = printFolderTree((PcloudFolder)rootFolder, "");
            System.out.println("Size: " + size+ " in " + (System.currentTimeMillis() - startTime) + "ms");

            System.out.println("======================================");
            System.out.println("Structure with fast-list: ");
            System.out.println("======================================");
            startTime = System.currentTimeMillis();
            rootFolder = pCloudManager.get(path, true);
            System.out.println("Size: " + size+ " in " + (System.currentTimeMillis() - startTime) + "ms");

            size = printFolderTree((PcloudFolder)rootFolder, "");
            System.out.println("Size: " + size);

            Path localPath = Paths.get("/home/jma/tmp/photosTest/2002/Pict200205010005.jpg");
            System.out.println("SHA1 Hash of file: " + SHA1.computeHash(localPath));

            File remoteFile = pCloudManager.get("/PhotosJM/2002/Pict200205010005.jpg", false);
            System.out.println("SHA1 Hash of remote file: " + remoteFile.getHash(SHA1));

            File remoteFile2 = pCloudManager.get("/testFuse.sh", false);
            System.out.println(remoteFile2);
        }
    }
    
    private static long printFolderTree(PcloudFolder folder, String indent) throws IOException {
        long size = 0;
        for (File entry : folder.list()) {
//            System.out.print(indent + "|");
            size ++;
            if (entry.isFile()) {
                String hash = WITH_HASH ? " (" + entry.getHash(SHA1) + ")" : "";
//                System.out.println("--- " + entry.getName() + " (" + entry.getSize() + "B - " + entry.getLastModified() + " - Hash: " + hash + ")");
            } else {
//                System.out.println("+-- " + entry.getName() + "/");
                size += printFolderTree((PcloudFolder)entry, indent + INDENT);
            }
        }
        return size;
    }
}
