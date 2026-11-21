package com.fathzer.sync4j.pcloud;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.Entry;
import com.fathzer.sync4j.Folder;
import com.fathzer.sync4j.pcloud.internal.api.PCloud;
import com.pcloud.sdk.RemoteFolder;

class PCloudFolderTest {
    private PCloudProvider provider;
    private PCloud pcloud;
    private PCloudFolder parent;

    @BeforeEach
    void setUp() {
        provider = mock(PCloudProvider.class);
        pcloud = mock(PCloud.class);
        when(provider.pCloud()).thenReturn(pcloud);
        RemoteFolder remoteParent = mock(RemoteFolder.class);
        when(remoteParent.isFolder()).thenReturn(true);
        when(remoteParent.asFolder()).thenReturn(remoteParent);
        when(remoteParent.folderId()).thenReturn(42L);
        when(remoteParent.name()).thenReturn("parent");
        parent = new PCloudFolder("", null, remoteParent, provider, false);
    }

    private static Entry entry(boolean file, boolean folder) {
        Entry entry = mock(Entry.class);
        when(entry.isFile()).thenReturn(file);
        when(entry.isFolder()).thenReturn(folder);
        return entry;
    }

    @Test
    void testMkdir() throws IOException {
        RemoteFolder created = mock(RemoteFolder.class);
        when(created.isFolder()).thenReturn(true);
        when(created.name()).thenReturn("child");
        Entry missing = entry(false, false);
        when(provider.get("/parent/child")).thenReturn(missing);
        when(pcloud.mkdir(42L, "child")).thenReturn(created);

        Folder folder = parent.mkdir("child");
        assertEquals("child", folder.getName());
        verify(pcloud).mkdir(42L, "child");
    }

    @Test
    void testMkdirWhenAFileHasTheSameName() throws IOException {
        Entry file = entry(true, false);
        when(provider.get("/parent/child")).thenReturn(file);

        assertThrows(FileAlreadyExistsException.class, () -> parent.mkdir("child"));
        verify(pcloud, never()).mkdir(anyLong(), anyString());
    }

    @Test
    void testMkdirIsIdempotent() throws IOException {
        Folder existing = mock(Folder.class);
        Entry existingEntry = entry(false, true);
        when(existingEntry.asFolder()).thenReturn(existing);
        when(provider.get("/parent/child")).thenReturn(existingEntry);

        assertSame(existing, parent.mkdir("child"));
        verify(pcloud, never()).mkdir(anyLong(), anyString());
    }

    @Test
    void testMkdirChecksFileName() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> parent.mkdir(""));
        assertThrows(IllegalArgumentException.class, () -> parent.mkdir("a/b"));
        verify(provider, never()).get(anyString());
    }
}
