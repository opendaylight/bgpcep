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

import java.io.File;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.bgpcep.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.mdsal.binding.dom.adapter.AdapterContext;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

public abstract class AbstractConfigLoaderTest extends AbstractConcurrentDataBrokerTest {
    protected final class TestConfigLoader extends AbstractConfigLoader {
        @Override
        public BindingNormalizedNodeSerializer getBindingNormalizedNodeSerializer() {
            return mappingService.currentSerializer();
        }

        @Override
        File directory() {
            return new File(getResourceFolder());
        }

        @Override
        @SuppressWarnings("checkStyle:illegalCatch")
        EffectiveModelContext modelContext() {
            try {
                return getSchemaContext();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to acquire schema context", e);
            }
        }

        public void triggerEvent(final String filename) {
            handleEvent(filename);
        }
    }

    protected final TestConfigLoader configLoader = new TestConfigLoader();
    @Mock
    ConfigFileProcessor processor;
    protected AdapterContext mappingService;
    protected DOMSchemaService schemaService;

    public AbstractConfigLoaderTest() {
        super(true);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doNothing().when(processor).loadConfiguration(any());
    }

    @Override
    protected AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        final AbstractDataBrokerTestCustomizer customizer = super.createDataBrokerTestCustomizer();
        this.mappingService = customizer.getAdapterContext();
        this.schemaService = customizer.getSchemaService();
        return customizer;
    }

    protected String getResourceFolder() {
        return ClassLoader.getSystemClassLoader().getResource("initial").getPath();
    }

}
