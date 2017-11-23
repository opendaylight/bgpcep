/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.protocol.bgp.config.loader.impl.AbstractConfigLoader;
import org.opendaylight.protocol.bgp.rib.impl.spi.BgpDeployer;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.NetworkInstances;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Protocol1;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class ProtocolsConfigFileProcessorTest extends AbstractConfigLoader {
    private static final InstanceIdentifier<NetworkInstance> NETWORK_INSTANCE_IID =
            InstanceIdentifier.create(NetworkInstances.class)
            .child(NetworkInstance.class, new NetworkInstanceKey("GLOBAL"));
    @Mock
    private BgpDeployer bgpDeployer;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        doReturn(NETWORK_INSTANCE_IID).when(this.bgpDeployer).getInstanceIdentifier();
        doNothing().when(this.bgpDeployer).onGlobalModified(any(), any(), any());
        doNothing().when(this.bgpDeployer).onNeighborModified(any(), any(), any());
    }

    @Override
    protected void registerModules(final ModuleInfoBackedContext moduleInfoBackedContext) throws Exception {
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(NetworkInstances.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(Protocols.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(Protocol.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(Protocol1.class));
    }

    @Override
    protected List<String> getYangModelsPaths() {
        return Arrays.asList(
            "/META-INF/yang/openconfig-extensions@2015-10-09.yang",
            "/META-INF/yang/bgp-openconfig-extensions@2016-06-14.yang",
            "/META-INF/yang/ietf-interfaces@2014-05-08.yang",
            "/META-INF/yang/openconfig-network-instance-types@2015-10-18.yang",
            "/META-INF/yang/openconfig-interfaces@2016-04-12.yang",
            "/META-INF/yang/openconfig-network-instance@2015-10-18.yang",
            "/META-INF/yang/openconfig-routing-policy@2015-10-09.yang",
            "/META-INF/yang/openconfig-policy-types@2015-10-09.yang",
            "/META-INF/yang/openconfig-local-routing@2015-10-09.yang",
            "/META-INF/yang/openconfig-bgp-operational@2015-10-09.yang",
            "/META-INF/yang/openconfig-bgp-types@2015-10-09.yang",
            "/META-INF/yang/openconfig-types@2015-10-09.yang",
            "/META-INF/yang/openconfig-bgp-multiprotocol@2015-10-09.yang",
            "/META-INF/yang/openconfig-bgp@2015-10-09.yang",
            "/META-INF/yang/bgp-rib@2013-09-25.yang",
            "/META-INF/yang/ietf-inet-types@2013-07-15.yang",
            "/META-INF/yang/bgp-message@2013-09-19.yang",
            "/META-INF/yang/bgp-multiprotocol@2013-09-19.yang",
            "/META-INF/yang/bgp-types@2013-09-19.yang",
            "/META-INF/yang/network-concepts@2013-11-25.yang",
            "/META-INF/yang/ieee754@2013-08-19.yang",
            "/META-INF/yang/ietf-yang-types@2013-07-15.yang",
            "/META-INF/yang/yang-ext@2013-07-09.yang"
        );
    }

    @Test
    public void configFileTest() throws Exception {
        assertNotNull(ClassLoader.getSystemClassLoader().getResource("initial/protocols-config.xml"));
        verify(this.bgpDeployer, never()).onGlobalModified(any(), any(), any());
        verify(this.bgpDeployer, never()).onNeighborModified(any(), any(), any());

        final ProtocolsConfigFileProcessor processor = new ProtocolsConfigFileProcessor(this.configLoader, this.bgpDeployer);
        assertEquals(SchemaPath.create(true, NetworkInstances.QNAME, NetworkInstance.QNAME, Protocols.QNAME), processor.getSchemaPath());

        verify(this.bgpDeployer).onGlobalModified(any(), any(), any());
        verify(this.bgpDeployer, times(2)).onNeighborModified(any(), any(), any());
        processor.close();
    }
}