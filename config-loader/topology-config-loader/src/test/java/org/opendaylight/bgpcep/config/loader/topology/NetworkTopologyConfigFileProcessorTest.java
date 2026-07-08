/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.config.loader.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.opendaylight.protocol.util.CheckUtil.checkNotPresentConfiguration;
import static org.opendaylight.protocol.util.CheckUtil.checkPresentConfiguration;

import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.bgpcep.config.loader.impl.AbstractConfigLoaderTest;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

public class NetworkTopologyConfigFileProcessorTest extends AbstractConfigLoaderTest {
    @Mock
    private ContainerNode node;

    @Test
    public void configFileTest() throws InterruptedException, ExecutionException {
        final var topologyIIdKeyed = DataObjectIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId("topology-test")))
            .build();
        checkNotPresentConfiguration(getDataBroker(), topologyIIdKeyed);

        assertNotNull(ClassLoader.getSystemClassLoader().getResource("initial/network-topology-config.xml"));
        try (var processor = new NetworkTopologyConfigFileProcessor(configLoader, getDomBroker())) {
            processor.init();
            checkPresentConfiguration(getDataBroker(), topologyIIdKeyed);

            assertEquals(Absolute.of(NetworkTopology.QNAME), processor.fileRootSchema());
        }
    }

    @Test
    public void emptyTopologiesTest() {
        doReturn(null).when(node).childByArg(new NodeIdentifier(Topology.QNAME));
        try (var processor = new NetworkTopologyConfigFileProcessor(configLoader, getDomBroker())) {
            final var future = processor.loadConfiguration(getDomBroker(), node);
            assertEquals(CommitInfo.emptyFluentFuture(), future);
        }
    }
}
