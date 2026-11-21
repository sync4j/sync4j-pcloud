package com.fathzer.sync4j.pcloud;

import java.io.IOException;

import com.fathzer.sync4j.FileProvider;
import com.fathzer.sync4j.FileProviderFactory;
import com.fathzer.sync4j.util.IOLambda.IOFunction;

import jakarta.annotation.Nonnull;

/**
 * The factory of the pCloud providers (type "pCloud"), to create them from a configuration (see {@link PCloudConfig}).
 * <p>
 * The factory is declared in the <code>META-INF/services/com.fathzer.sync4j.FileProviderFactory</code> file of this library:
 * a program that reads configuration files (see sync4j-config) can use pCloud as soon as this library is on its classpath.
 * </p>
 */
public class PCloudProviderFactory implements FileProviderFactory<PCloudConfig> {
    /** The type of the provider, the <code>provider</code> attribute of a location of a configuration. */
    // False positive: TYPE is not a clash with type(), it is the value that type() returns
    @SuppressWarnings("java:S1845")
    public static final String TYPE = "pCloud";

    @Override
    @Nonnull
    public String type() {
        return TYPE;
    }

    @Override
    @Nonnull
    public Class<PCloudConfig> configType() {
        return PCloudConfig.class;
    }

    @Override
    @Nonnull
    public IOFunction<String, FileProvider> create(PCloudConfig config) throws IOException {
        if (config.zone() == null) {
            throw new IOException("The configuration of a pCloud provider needs a zone (us or eu)");
        }
        if (config.token() == null || config.token().isBlank()) {
            throw new IOException("The configuration of a pCloud provider needs a token");
        }
        return root -> new PCloudProvider(config.zone(), config.token(), root);
    }
}
