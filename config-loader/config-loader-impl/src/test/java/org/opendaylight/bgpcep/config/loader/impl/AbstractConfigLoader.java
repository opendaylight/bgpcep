/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.bgpcep.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.mdsal.binding.dom.adapter.AdapterContext;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;

public abstract class AbstractConfigLoader extends AbstractConcurrentDataBrokerTest {
    @GuardedBy("this")
    private final List<WatchEvent<?>> eventList = new CopyOnWriteArrayList<>();
    protected AbstractConfigLoader configLoader;
    @Mock
    private WatchService watchService;
    @Mock
    ConfigFileProcessor processor;
    @Mock
    private WatchKey watchKey;
    @Mock
    private WatchEvent<?> watchEvent;
    @Mock
    private FileWatcher fileWatcher;
    protected AdapterContext mappingService;
    protected DOMSchemaService schemaService;

    public AbstractConfigLoader() {
        super(true);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doAnswer(invocation -> true).when(this.watchKey).reset();
        doReturn(this.eventList).when(this.watchKey).pollEvents();
        doReturn(this.watchKey).when(this.watchService).take();
        doReturn("watchKey").when(this.watchKey).toString();
        doReturn("watchService").when(this.watchService).toString();
        doReturn("watchEvent").when(this.watchEvent).toString();
        doReturn(getResourceFolder()).when(this.fileWatcher).getPathFile();
        doReturn(this.watchService).when(this.fileWatcher).getWatchService();
        doAnswer(invocation -> {
            clearEvent();
            return null;
        }).when(this.processor).loadConfiguration(any());
        this.configLoader = new AbstractConfigLoader(getSchemaContext(), this.mappingService.currentSerializer(),
            this.fileWatcher);
        this.configLoader.init();
    }

    @Override
    protected final AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        final AbstractDataBrokerTestCustomizer customizer = super.createDataBrokerTestCustomizer();
        this.mappingService = customizer.getAdapterContext();
        this.schemaService = customizer.getSchemaService();
        return customizer;
    }

    private synchronized void clearEvent() {
        this.eventList.clear();
    }

    protected String getResourceFolder() {
        return ClassLoader.getSystemClassLoader().getResource("initial").getPath();
    }

    protected synchronized void triggerEvent(final String filename) {
        doReturn(filename).when(this.watchEvent).context();
        this.eventList.add(this.watchEvent);
    }

    @After
    public void tearDown() throws Exception {
        this.configLoader.close();
    }
}
