/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.config.loader.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javassist.ClassPool;
import javax.annotation.concurrent.GuardedBy;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.protocol.bgp.config.loader.spi.ConfigFileProcessor;
import org.opendaylight.protocol.bgp.config.loader.spi.ConfigLoader;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangInferencePipeline;

public abstract class AbstractConfigLoader {
    @GuardedBy("this")
    private final List<WatchEvent> eventList = new ArrayList<>();
    protected BindingToNormalizedNodeCodec mappingService;
    protected ConfigLoader configLoader;
    @Mock
    protected WatchService watchService;
    @Mock
    private WatchKey watchKey;
    @Mock
    private WatchEvent watchEvent;
    @Mock
    protected ConfigFileProcessor processor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.mappingService = new BindingToNormalizedNodeCodec(GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy(),
            new BindingNormalizedNodeCodecRegistry(StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault()))));
        final ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();
        registerModules(moduleInfoBackedContext);
        this.mappingService.onGlobalContextUpdated(moduleInfoBackedContext.tryToCreateSchemaContext().get());
        doAnswer(invocation -> {
           // clearEvent();
            return true;
        }).when(this.watchKey).reset();
        doReturn(this.eventList).when(this.watchKey).pollEvents();
        doReturn(this.watchKey).when(this.watchService).take();
        doReturn("watchKey").when(this.watchKey).toString();
        doReturn("watchService").when(this.watchService).toString();
        doReturn("watchEvent").when(this.watchEvent).toString();
        doAnswer(invocation -> {
            clearEvent();
            return null;
        }).when(this.processor).loadConfiguration(any());
        final SchemaContext schemaContext = parseYangStreams(getFilesAsByteSources(getYangModelsPaths()));
        this.configLoader = new ConfigLoaderImpl(schemaContext, this.mappingService, getResourceFolder(), this.watchService);
    }

    protected synchronized void clearEvent() {
        this.eventList.clear();
    }

    protected abstract String getResourceFolder();

    protected abstract void registerModules(final ModuleInfoBackedContext moduleInfoBackedContext) throws Exception;

    protected abstract List<String> getYangModelsPaths();

    private Collection<ByteSource> getFilesAsByteSources(final List<String> paths) {
        final Collection<ByteSource> resources = new ArrayList<>();
        final List<String> failedToFind = new ArrayList<>();
        for (final String path : paths) {
            final URL url = ConfigLoaderImplTest.class.getResource(path);
            if (url == null) {
                failedToFind.add(path);
            } else {
                resources.add(Resources.asByteSource(url));
            }
        }
        Assert.assertEquals("Some files were not found", Collections.<String>emptyList(), failedToFind);
        return resources;
    }

    private static SchemaContext parseYangStreams(final Collection<ByteSource> streams) {
        final CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();
        try {
            return reactor.buildEffective(streams);
        } catch (final ReactorException | IOException e) {
            throw new RuntimeException("Unable to build schema context from " + streams, e);
        }
    }

    protected synchronized void triggerEvent(final String filename) {
        doReturn(filename).when(this.watchEvent).context();
        this.eventList.add(this.watchEvent);
    }

    @After
    public final void tearDown() throws Exception {
        ((ConfigLoaderImpl) this.configLoader).close();
    }
}
