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
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class ConfigLoaderImplTest extends SimpleConfigLoader {
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        final SchemaPath schemaPath = SchemaPath.create(true,
                NetworkInstances.QNAME, NetworkInstance.QNAME, Protocols.QNAME);
        doReturn(schemaPath).when(this.processor).getSchemaPath();
        doReturn("processor").when(this.processor).toString();
    }

    @Override
    protected String getResourceFolder() {
        return ClassLoader.getSystemClassLoader().getResource("etc/opendaylight/bgpcep").getPath();
    }

    @Test
    public void configLoaderImplTest() {
        assertNotNull(ClassLoader.getSystemClassLoader()
                .getResource("etc/opendaylight/bgpcep/protocols-config.xml"));
        final AbstractRegistration ticket = this.configLoader.registerConfigFile(this.processor);
        verify(this.processor).loadConfiguration(any());

        triggerEvent("protocols-config.xml");
        verify(this.processor, timeout(20000).times(2)).loadConfiguration(any());

        ticket.close();
        triggerEvent("protocols-config.xml");
        verify(this.processor, timeout(20000).times(2)).loadConfiguration(any());
    }
}