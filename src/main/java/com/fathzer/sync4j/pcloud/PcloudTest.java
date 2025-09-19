package com.fathzer.sync4j.pcloud;

import static com.fathzer.sync4j.HashAlgorithm.SHA1;

import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.pcloud.sdk.ApiClient;
import com.pcloud.sdk.Authenticators;
import com.pcloud.sdk.PCloudSdk;
import com.pcloud.sdk.RemoteEntry;
import com.pcloud.sdk.RemoteFolder;
import com.pcloud.sdk.RemoteFile;

public class PcloudTest {
    private static final String INDENT = "    ";
    
    public static void main(String[] args) throws Exception {
        final String accessToken = args[0];
        try (PCloudProvider pCloudManager = new PCloudProvider(accessToken)) {

            ApiClient apiClient = PCloudSdk.newClientBuilder()
                    .authenticator(Authenticators.newOAuthAuthenticator(accessToken))
                    .create();
            try {
                // RemoteFolder rootFolder = apiClient.listFolder("/PhotosJM/2002", true).execute();
                // System.out.println("Remote Folder Structure for: " + rootFolder.name());
                // System.out.println("======================================");
                // printFolderTree(rootFolder, "");


                Path localPath = Paths.get("/home/jma/tmp/photosTest/2002/Pict200205010005.jpg");
                System.out.println("SHA1 Hash of file: " + SHA1.computeHash(localPath));

                RemoteFile remoteFile = apiClient.loadFile("/PhotosJM/2002/Pict200205010005.jpg").execute();
                System.out.println("SHA1 Hash of remote file: " + pCloudManager.getHash(new PcloudFile(remoteFile), SHA1));

                remoteFile = apiClient.loadFile("/testFuse.sh").execute();
                System.out.println(remoteFile);
                

            } catch (Exception e) {
                System.err.println("Error accessing pCloud: " + e.getMessage());
                e.printStackTrace();
            } finally {
                apiClient.shutdown();
            }
        }
    }
    
    private static void printFolderTree(RemoteFolder folder, String indent) {
        if (folder == null || folder.children() == null) return;
        
        for (RemoteEntry entry : folder.children()) {
            System.out.print(indent + "|");
            if (entry.isFile()) {
                RemoteFile file = entry.asFile();
                String hash = file.hash();
                hash = hash == null ? "no hash" : new BigInteger(hash).toString(16);
                System.out.println("--- " + file.name() + " (" + file.size() + "B - " + file.lastModified() + " - Hash: " + hash + ")");
            } else if (entry.isFolder()) {
                RemoteFolder subFolder = entry.asFolder();
                System.out.println("+-- " + subFolder.name() + "/");
                printFolderTree(subFolder, indent + INDENT);
            }
        }
    }
    
    private static String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", size / Math.pow(1024, exp), pre);
    }
}
