/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.impl;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URL;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

public class ConfigLoaderImplTest extends AbstractConfigLoaderTest {
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        doReturn(Absolute.of(NetworkInstances.QNAME, NetworkInstance.QNAME, Protocols.QNAME)).when(processor)
            .fileRootSchema();
        doReturn("processor").when(processor).toString();
    }

    @Override
    protected URL getResourceFolder() {
        return ClassLoader.getSystemClassLoader().getResource("etc/opendaylight/bgpcep");
    }

    @Test
    public void configLoaderImplTest() {
        assertNotNull(ClassLoader.getSystemClassLoader()
                .getResource("etc/opendaylight/bgpcep/protocols-config.xml"));

        try (AbstractRegistration ticket = this.configLoader.registerConfigFile(processor)) {
            verify(processor).loadConfiguration(any());

            configLoader.triggerEvent("protocols-config.xml");
            verify(processor, times(2)).loadConfiguration(any());
        }

        configLoader.triggerEvent("protocols-config.xml");
        verify(processor, times(2)).loadConfiguration(any());
    }
}
