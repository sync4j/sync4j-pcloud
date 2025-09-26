package com.fathzer.sync4j.pcloud.internal;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.pcloud.PCloudProvider;

class PathUtilsTest {
    @Test
    void testIsRoot() {
        assertTrue(PathUtils.isRoot(PCloudProvider.ROOT_PATH));
        assertFalse(PathUtils.isRoot("/folder"));
    }

    @Test
    void testGetParent() {
        assertEquals(PCloudProvider.ROOT_PATH, PathUtils.getParent("/folder"));
        assertEquals("/folder", PathUtils.getParent("/folder/file"));
        assertNull(PathUtils.getParent(""));
    }

    @Test
    void testGetName() {
        assertEquals("folder", PathUtils.getName("/folder"));
        assertEquals("file", PathUtils.getName("/folder/file"));
        assertEquals("", PathUtils.getName(PCloudProvider.ROOT_PATH));
    }
}
