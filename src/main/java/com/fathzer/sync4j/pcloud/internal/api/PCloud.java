package com.fathzer.sync4j.pcloud.internal.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.LongConsumer;

import com.fathzer.sync4j.HashAlgorithm;
import com.pcloud.sdk.RemoteEntry;
import com.pcloud.sdk.RemoteFile;
import com.pcloud.sdk.RemoteFolder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Interface to the pCloud API.
 */
public interface PCloud extends AutoCloseable {
    /**
     * Get a remote entry.
     * @param path the path of the remote entry. All paths should start with a slash except the root path which is an empty string.
     * @return the remote entry
     * @throws IOException if an I/O error occurs
     */
	@Nonnull
    RemoteEntry get(@Nonnull String path) throws IOException;

    /**
     * Get the hash of a remote file.
     * @param remoteFile the remote file
     * @param hashAlgorithm the hash algorithm
     * @return the hash of the remote file
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if hash is not supported
     */
    @Nonnull
    String getHash(@Nonnull RemoteFile remoteFile, @Nonnull HashAlgorithm hashAlgorithm) throws IOException;

    /**
     * Get the input stream of a remote file.
     * @param remoteFile the remote file
     * @return the input stream of the remote file
     * @throws IOException if an I/O error occurs
     */
    @Nonnull
    InputStream getInputStream(@Nonnull RemoteFile remoteFile) throws IOException;

    /**
     * List the content of a remote folder.
     * @param folderId the ID of the remote folder
     * @param recursive if true, list the content of the remote folder recursively
     * @return the content of the remote folder
     * @throws IOException if an I/O error occurs
     */
    @Nonnull
    RemoteFolder listFolder(long folderId, boolean recursive) throws IOException;

    /**
     * Delete a remote entry.
     * <br>If the entry is a folder, it is deleted recursively.
     * @param remoteEntry the remote entry to delete
     * @throws IOException if an I/O error occurs
     */
    void delete(@Nonnull RemoteEntry remoteEntry) throws IOException;

    /**
     * Upload a file to a remote folder.
     * @param folderId the ID of the remote folder
     * @param fileName the name of the file to upload
     * @param content the content of the file to upload
     * @param size the size of the file to upload
     * @param mtime the modification time of the file to upload
     * @param ctime the creation time of the file to upload
     * @param progressListener the progress listener, or null if there's no progress listener
     * @return the remote file
     * @throws IOException if an I/O error occurs
     */
    @Nonnull
    RemoteFile upload(long folderId, @Nonnull String fileName, @Nonnull InputStream content, long size, long mtime, long ctime, @Nullable LongConsumer progressListener) throws IOException;

    /**
     * Create a remote folder.
     * @param folderId the ID of the remote folder
     * @param folderName the name of the remote folder to create
     * @return the remote folder
     * @throws IOException if an I/O error occurs
     */
    @Nonnull
    RemoteFolder mkdir(long folderId, @Nonnull String folderName) throws IOException;
    
    @Override
    void close();
}
