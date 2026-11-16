package com.fathzer.sync4j.pcloud;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;

import com.fathzer.sync4j.FileProviderFactory;

class PCloudProviderFactoryTest {
    private final PCloudProviderFactory factory = new PCloudProviderFactory();

    private IOException failure(PCloudConfig config) {
        return assertThrows(IOException.class, () -> factory.create(config));
    }

    @Test
    void testType() {
        assertEquals("pCloud", factory.type());
        assertEquals(PCloudProviderFactory.TYPE, factory.type());
        assertEquals(PCloudConfig.class, factory.configType());
    }

    @Test
    void testValidConfiguration() throws IOException {
        // The provider is only opened (and the token only used) when the function is called
        assertNotNull(factory.create(new PCloudConfig(Zone.US, "t")));
        assertNotNull(factory.create(new PCloudConfig(Zone.EU, "t")));
    }

    @Test
    void testInvalidConfigurations() {
        assertTrue(failure(new PCloudConfig(null, "t")).getMessage().contains("zone"));
        assertTrue(failure(new PCloudConfig(Zone.US, null)).getMessage().contains("token"));
        assertTrue(failure(new PCloudConfig(Zone.US, " ")).getMessage().contains("token"));
        assertTrue(failure(new PCloudConfig(null, null)).getMessage().contains("zone"));
    }

    @Test
    void testTheTokenIsNotInTheStringRepresentation() {
        final String text = new PCloudConfig(Zone.US, "very-secret-token").toString();

        assertFalse(text.contains("very-secret-token"), text);
        assertTrue(text.contains("US"), text);
    }

    @Test
    void testFactoryIsDeclaredAsAService() {
        // This is how a program that reads configuration files finds the factory, without depending on this library
        final List<FileProviderFactory<?>> factories = new ArrayList<>();
        @SuppressWarnings("rawtypes")
        final ServiceLoader<FileProviderFactory> loader = ServiceLoader.load(FileProviderFactory.class);
        loader.forEach(factories::add);

        // The ones of sync4j (local, memory) and this one
        assertTrue(factories.stream().anyMatch(f -> f instanceof PCloudProviderFactory), factories.toString());
        assertEquals(1, factories.stream().filter(f -> f.type().equals("pCloud")).count());
    }
}
