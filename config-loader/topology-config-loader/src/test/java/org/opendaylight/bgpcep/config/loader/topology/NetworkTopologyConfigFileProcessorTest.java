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
import static org.opendaylight.protocol.util.CheckTestUtil.checkNotPresentConfiguration;
import static org.opendaylight.protocol.util.CheckTestUtil.checkPresentConfiguration;

import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.bgpcep.config.loader.impl.AbstractConfigLoader;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class NetworkTopologyConfigFileProcessorTest extends AbstractConfigLoader {
    @Test
    public void configFileTest() throws InterruptedException, ExecutionException {
        final KeyedInstanceIdentifier<Topology, TopologyKey> topologyIIdKeyed =
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class,
                        new TopologyKey(new TopologyId("topology-test")));
        checkNotPresentConfiguration(getDataBroker(), topologyIIdKeyed);

        assertNotNull(ClassLoader.getSystemClassLoader().getResource("initial/network-topology-config.xml"));
        final NetworkTopologyConfigFileProcessor processor = new NetworkTopologyConfigFileProcessor(this.configLoader,
                getDataBroker());
        processor.init();
        checkPresentConfiguration(getDataBroker(), topologyIIdKeyed);

        assertEquals(SchemaPath.create(true, NetworkTopology.QNAME), processor.getSchemaPath());
        processor.close();
    }
}