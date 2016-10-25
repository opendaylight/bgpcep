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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import javassist.ClassPool;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
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
    private final List<WatchEvent> eventList = new ArrayList<>();
    protected SchemaContext schemaContext;
    protected BindingToNormalizedNodeCodec mappingService;
    protected ConfigLoaderImpl configLoader;
    protected Function<String, Void> newFileFunctionHandler;
    @Mock
    protected FileWatcher fileWatcher;
    @Mock
    private WatchKey watchKey;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.mappingService = new BindingToNormalizedNodeCodec(GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy(),
            new BindingNormalizedNodeCodecRegistry(StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault()))));
        final ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();
        registerModules(moduleInfoBackedContext);
        this.mappingService.onGlobalContextUpdated(moduleInfoBackedContext.tryToCreateSchemaContext().get());
        this.schemaContext = parseYangStreams(getFilesAsByteSources(getYangModelsPaths()));
        doAnswer(invocation -> {
            this.eventList.clear();
            return true;
        }).when(this.watchKey).reset();
        doReturn(this.eventList).when(this.watchKey).pollEvents();
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) {
                final Object[] args = invocation.getArguments();
                AbstractConfigLoader.this.newFileFunctionHandler = ((Function<String, Void>) args[0]);
                return null;
            }
        }).when(this.fileWatcher).handleEvents(any());
        doReturn(getResourceFolder()).when(this.fileWatcher).getPathFile();
        this.configLoader = new ConfigLoaderImpl(this.schemaContext, this.mappingService, this.fileWatcher);
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

    @After
    public final void tearDown() throws Exception {
        this.configLoader.close();
    }
}
