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

import java.nio.file.Path;
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
            return Path.of(getResourceFolder());
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
        try (var mock = MockitoAnnotations.openMocks(this)) {
            doNothing().when(processor).loadConfiguration(any());
            configLoader.updateModelContext(modelContext());
        }
    }

    @Override
    protected AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        final var customizer = super.createDataBrokerTestCustomizer();
        schemaService = customizer.getSchemaService();
        return customizer;
    }

    protected String getResourceFolder() {
        return ClassLoader.getSystemClassLoader().getResource("initial").getPath();
    }
}
