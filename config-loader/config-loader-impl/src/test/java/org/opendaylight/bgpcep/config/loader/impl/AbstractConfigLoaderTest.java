/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.bgpcep.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;

public abstract class AbstractConfigLoaderTest extends AbstractConcurrentDataBrokerTest {
    protected final class TestConfigLoader extends AbstractConfigLoader {
        @Override
        Path directory() {
            final URI uri;
            try {
                uri = getResourceFolder().toURI();
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }

            // from https://stackoverflow.com/a/48298758
            if ("jar".equals(uri.getScheme())) {
                for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
                    if (provider.getScheme().equalsIgnoreCase("jar")) {
                        try {
                            provider.getFileSystem(uri);
                        } catch (FileSystemNotFoundException e) {
                            // in this case we need to initialize it first:
                            try {
                                provider.newFileSystem(uri, Map.of());
                            } catch (IOException io) {
                                throw new IllegalStateException(io);
                            }
                        }
                    }
                }
            }

            return Path.of(uri);
        }

        public void triggerEvent(final String filename) {
            handleEvent(filename);
        }
    }

    protected final TestConfigLoader configLoader = new TestConfigLoader();

    @Mock
    ConfigFileProcessor processor;
    protected DOMSchemaService schemaService;

    public AbstractConfigLoaderTest() {
        super(true);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doNothing().when(processor).loadConfiguration(any());
        configLoader.updateModelContext(getSchemaContext());
    }

    @Override
    protected AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        final AbstractDataBrokerTestCustomizer customizer = super.createDataBrokerTestCustomizer();
        this.schemaService = customizer.getSchemaService();
        return customizer;
    }

    protected URL getResourceFolder() {
        return ClassLoader.getSystemClassLoader().getResource("initial");
    }
}
