/*
 * Copyright (c) 2017 AT&T Intellectual Property.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.protocol.bgp.config.loader.impl.AbstractConfigLoader;
import org.opendaylight.protocol.bmp.impl.api.BmpDeployer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev170517.OdlBmpMonitors;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class BmpMonitorConfigFileProcessorTest extends AbstractConfigLoader {
    @Mock
    private BmpDeployer bmpDeployer;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void registerModules(final ModuleInfoBackedContext moduleInfoBackedContext) throws Exception {
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(OdlBmpMonitors.class));

    }

    @Override
    protected List<String> getYangModelsPaths() {
        final List<String> paths = Lists.newArrayList(
                "/META-INF/yang/odl-bmp-monitor-config.yang",
                "/META-INF/yang/ietf-inet-types@2013-07-15.yang",
                "/META-INF/yang/ietf-yang-types@2013-07-15.yang",
                "/META-INF/yang/bgp-rib.yang",
                "/META-INF/yang/bgp-multiprotocol.yang",
                "/META-INF/yang/bgp-types.yang",
                "/META-INF/yang/network-concepts.yang",
                "/META-INF/yang/ieee754.yang",
                "/META-INF/yang/bgp-message.yang",
                "/META-INF/yang/yang-ext.yang",
                "/META-INF/yang/bmp-monitor.yang",
                "/META-INF/yang/bmp-message.yang",
                "/META-INF/yang/rfc2385.yang"
        );
        return paths;
    }

    @Test
    public void configFileTest() throws Exception {
        assertNotNull(ClassLoader.getSystemClassLoader().getResource("initial/odl-bmp-monitors-config.xml"));
        verify(this.bmpDeployer, never()).writeBmpMonitor(any());
        doNothing().when(this.bmpDeployer).writeBmpMonitor(any());
        final BmpMonitorConfigFileProcessor processor =
                new BmpMonitorConfigFileProcessor(this.configLoader, this.bmpDeployer);
        processor.register();
        assertEquals(SchemaPath.create(true, OdlBmpMonitors.QNAME), processor.getSchemaPath());

        verify(this.bmpDeployer).writeBmpMonitor(any());
        processor.close();
    }
}