package com.fathzer.sync4j.pcloud;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.pcloud.sdk.RemoteEntry;

class PCloudEntryTest {

	@Test
	void testPCloudFolderConstructor() {
		PCloudProvider provider = mock(PCloudProvider.class, CALLS_REAL_METHODS);
		RemoteEntry entry = mock(RemoteEntry.class);
		assertThrows(IllegalArgumentException.class, () -> new PCloudFolder(null, null, entry, provider, false), "Remote entry that is not folder should be rejected");
		
		lenient().when(entry.isFolder()).thenReturn(true);
		assertThrows(IllegalArgumentException.class, () -> new PCloudFolder("//toto", null, entry, provider, false), "Invalid path should be rejected");

		PCloudFolder parent = mock(PCloudFolder.class);
		assertThrows(IllegalArgumentException.class, () -> new PCloudFolder(null, parent, entry, provider, false), "non null parent with null parentPath should be rejected");

		assertDoesNotThrow(() -> new PCloudFolder(null, null, entry, provider, false));
		assertDoesNotThrow(() -> new PCloudFolder("/folder", null, entry, provider, false));
		
		assertThrows(NullPointerException.class, () -> new PCloudFolder(null, null, null, provider, false), "Null remote entry should be rejected");
		assertThrows(NullPointerException.class, () -> new PCloudFolder(null, null, entry, null, false), "Null provider should be rejected");
	}
	
	@Test
	void testPCloudFileConstructor() {
		PCloudProvider provider = mock(PCloudProvider.class, CALLS_REAL_METHODS);
		RemoteEntry entry = mock(RemoteEntry.class);

		assertThrows(IllegalArgumentException.class, () -> new PCloudFile("", null, entry, provider), "Remote entry that is not file should be rejected");

		lenient().when(entry.isFile()).thenReturn(true);
		assertThrows(IllegalArgumentException.class, () -> new PCloudFile("//toto", null, entry, provider), "Invalid path should be rejected");

		assertDoesNotThrow(() -> new PCloudFile("/folder", null, entry, provider));

		assertThrows(NullPointerException.class, () -> new PCloudFile("/folder", null, null, provider), "Null remote entry should be rejected");
		assertThrows(NullPointerException.class, () -> new PCloudFile(null, null, entry, null), "Null provider should be rejected");
	}
}
