/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.impl;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class OSGiConfigLoaderTest {
    @Mock
    private FileWatcher watcher;
    @Mock
    private WatchService watchService;
    @Mock
    private WatchKey watchKey;
    @Mock
    private WatchEvent<?> watchEvent;
    @Mock
    private BindingRuntimeContext bindingContext;
    @Mock
    private EffectiveModelContext domContext;

    private OSGiConfigLoader loader;

    @Before
    public void before() throws InterruptedException {
        doReturn(watchService).when(watcher).getWatchService();
        doReturn("foo").when(watcher).getPathFile();
        doReturn(watchKey).when(watchService).take();
        doAnswer(inv -> {
            doThrow(new RuntimeException("enough!")).when(watchKey).pollEvents();
            return List.of(watchEvent);
        }).when(watchKey).pollEvents();
        doReturn("watchEvent").when(watchEvent).context();
        doReturn(true).when(watchKey).reset();
        doReturn(domContext).when(bindingContext).getEffectiveModelContext();

        loader = new OSGiConfigLoader();
        loader.watcher = watcher;
        loader.setRuntimeContext(bindingContext);
    }

    @After
    public void after() {
        loader.deactivate();
    }

    @Test
    public void testSimpleConfigLoader() {
        loader.activate();
        verify(watchKey, timeout(10000)).reset();
    }
}
