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
import static org.opendaylight.protocol.util.CheckUtil.checkNotPresentConfiguration;
import static org.opendaylight.protocol.util.CheckUtil.checkPresentConfiguration;

import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.bgpcep.config.loader.impl.AbstractConfigLoaderTest;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

public class NetworkTopologyConfigFileProcessorTest extends AbstractConfigLoaderTest {
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
}
