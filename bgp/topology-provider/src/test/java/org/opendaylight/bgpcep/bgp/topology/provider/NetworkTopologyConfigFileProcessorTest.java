/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyDeployer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.protocol.bgp.config.loader.impl.AbstractConfigLoader;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class NetworkTopologyConfigFileProcessorTest extends AbstractConfigLoader {
    private static final InstanceIdentifier<Topology> TOPOLOGY_IID = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class);
    @Mock
    private BgpTopologyDeployer bgpDeployer;
    @Mock
    private DataBroker dataBroker;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        doReturn(TOPOLOGY_IID).when(this.bgpDeployer).getInstanceIdentifier();
        doNothing().when(this.bgpDeployer).createInstance(any());
        doReturn(this.dataBroker).when(this.bgpDeployer).getDataBroker();

    }

    @Override
    protected void registerModules(final ModuleInfoBackedContext moduleInfoBackedContext) throws Exception {
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(NetworkTopology.class));
        moduleInfoBackedContext.registerModuleInfo(BindingReflections.getModuleInfo(Topology.class));
    }

    @Override
    protected List<String> getYangModelsPaths() {
        final List<String> paths = Lists.newArrayList(
            "/META-INF/yang/network-topology@2013-10-21.yang",
            "/META-INF/yang/ietf-inet-types@2013-07-15.yang",
            "/META-INF/yang/odl-bgp-topology-types.yang",
            "/META-INF/yang/odl-bgp-topology-config.yang",
            "/META-INF/yang/bgp-rib.yang",
            "/META-INF/yang/bgp-multiprotocol.yang",
            "/META-INF/yang/bgp-types.yang",
            "/META-INF/yang/network-concepts.yang",
            "/META-INF/yang/ieee754.yang",
            "/META-INF/yang/bgp-message.yang",
            "/META-INF/yang/yang-ext.yang"
        );
        return paths;
    }

    @Test
    public void configFileTest() throws Exception {
        assertNotNull(ClassLoader.getSystemClassLoader().getResource("initial/network-topology-config.xml"));
        verify(this.bgpDeployer, never()).createInstance(any());
        final NetworkTopologyConfigFileProcessor processor = new NetworkTopologyConfigFileProcessor(this.configLoader, this.bgpDeployer);
        assertEquals(SchemaPath.create(true, NetworkTopology.QNAME), processor.getSchemaPath());

        verify(this.bgpDeployer, times(3)).createInstance(any());
        processor.close();
    }
}