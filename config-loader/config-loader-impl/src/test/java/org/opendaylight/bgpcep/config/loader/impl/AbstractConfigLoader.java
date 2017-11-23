/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.config.loader.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import javassist.ClassPool;
import javax.annotation.concurrent.GuardedBy;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.bgpcep.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public abstract class AbstractConfigLoader extends AbstractConcurrentDataBrokerTest {
    @GuardedBy("this")
    private final List<WatchEvent<?>> eventList = new ArrayList<>();
    protected ConfigLoaderImpl configLoader;
    @Mock
    protected WatchService watchService;
    @Mock
    private WatchKey watchKey;
    @Mock
    private WatchEvent<?> watchEvent;
    @Mock
    protected ConfigFileProcessor processor;
    @Mock
    private FileWatcher fileWatcher;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final BindingToNormalizedNodeCodec mappingService = new BindingToNormalizedNodeCodec(
                GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy(),
                new BindingNormalizedNodeCodecRegistry(
                        StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault()))));
        final ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();
        registerModules(moduleInfoBackedContext);
        mappingService.onGlobalContextUpdated(moduleInfoBackedContext.tryToCreateSchemaContext().get());
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
        final SchemaContext schemaContext = getSchemaContext();
        this.configLoader = new ConfigLoaderImpl(schemaContext,
                mappingService, this.fileWatcher);
        this.configLoader.init();
    }

    private synchronized void clearEvent() {
        this.eventList.clear();
    }

    protected String getResourceFolder() {
        return ClassLoader.getSystemClassLoader().getResource("initial").getPath();
    }

    protected abstract void registerModules(ModuleInfoBackedContext moduleInfoBackedContext) throws Exception;

    protected synchronized void triggerEvent(final String filename) {
        doReturn(filename).when(this.watchEvent).context();
        this.eventList.add(this.watchEvent);
    }

    @After
    public final void tearDown() throws Exception {
        ((ConfigLoaderImpl) this.configLoader).close();
    }
}
