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

import com.google.common.collect.Lists;
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
        final List<String> paths = Lists.newArrayList(
                "/META-INF/yang/openconfig-extensions.yang",
                "/META-INF/yang/bgp-openconfig-extensions.yang",
                "/META-INF/yang/ietf-interfaces.yang",
                "/META-INF/yang/openconfig-network-instance-types.yang",
                "/META-INF/yang/openconfig-interfaces.yang",
                "/META-INF/yang/openconfig-network-instance.yang",
                "/META-INF/yang/openconfig-routing-policy.yang",
                "/META-INF/yang/openconfig-policy-types.yang",
                "/META-INF/yang/openconfig-local-routing.yang",
                "/META-INF/yang/openconfig-bgp-operational.yang",
                "/META-INF/yang/openconfig-bgp-types.yang",
                "/META-INF/yang/openconfig-types.yang",
                "/META-INF/yang/openconfig-bgp-multiprotocol.yang",
                "/META-INF/yang/openconfig-bgp.yang",
                "/META-INF/yang/bgp-rib.yang",
                "/META-INF/yang/ietf-inet-types@2013-07-15.yang",
                "/META-INF/yang/bgp-message.yang",
                "/META-INF/yang/bgp-multiprotocol.yang",
                "/META-INF/yang/bgp-types.yang",
                "/META-INF/yang/network-concepts.yang",
                "/META-INF/yang/ieee754.yang",
                "/META-INF/yang/ietf-yang-types@2013-07-15.yang",
                "/META-INF/yang/yang-ext.yang"
        );
        return paths;
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