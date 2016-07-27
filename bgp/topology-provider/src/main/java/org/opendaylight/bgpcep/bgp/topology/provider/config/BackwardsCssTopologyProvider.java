/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider.config;

import com.google.common.base.Optional;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.lang.reflect.Method;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.TopologyReferenceSingletonService;
import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.config.rev160726.Topology1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.config.rev160726.Topology1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BackwardsCssTopologyProvider {

    private static final Logger LOG = LoggerFactory.getLogger(BackwardsCssTopologyProvider.class);

    public static TopologyReferenceSingletonService createBackwardsCssInstance(final TopologyTypes topologyTypes, final TopologyId topologyId, final DataBroker dataBroker, final BundleContext bundleContext,
            final KeyedInstanceIdentifier<Rib, RibKey> ribIId) {
        //map configuration to topology
        final Topology topology = createConfiguration(topologyTypes, topologyId, ribIId.getKey().getId());
        //write to configuration DS
        final KeyedInstanceIdentifier<Topology, TopologyKey> topologyIId = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, topology.getKey());
        final ReadWriteTransaction rwTx = dataBroker.newReadWriteTransaction();
        //check if topology configuration is not already present
        Futures.addCallback(rwTx.read(LogicalDatastoreType.CONFIGURATION, topologyIId), new FutureCallback<Optional<Topology>>() {
            @Override
            public void onSuccess(final Optional<Topology> result) {
                if (!result.isPresent()) {
                    writeConfiguration(rwTx, topologyIId, topology);
                }
                rwTx.submit();
            }
            @Override
            public void onFailure(final Throwable t) {
                LOG.warn("Failed to ensure topology {} configuration presence, try to write the configuration anyway.", topologyId, t);
                writeConfiguration(rwTx, topologyIId, topology);
                rwTx.submit();
            }
        });
        //get topology service, use filter
        final WaitingServiceTracker<TopologyReference> topologyTracker = WaitingServiceTracker.create(TopologyReference.class,
                bundleContext, "(" + "topology-id" + "=" + topology.getTopologyId().getValue() + ")");
        final TopologyReference topologyService = topologyTracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);
        return Reflection.newProxy(TopologyReferenceSingletonService.class, new AbstractInvocationHandler() {
            @Override
            protected Object handleInvocation(final Object proxy, final Method method, final Object[] args) throws Throwable {
                if (method.getName().equals("close")) {
                    topologyTracker.close();
                    return null;
                } else {
                    return method.invoke(topologyService, args);
                }
            }
        });
    }

    private static Topology createConfiguration(final TopologyTypes topologyTypes, final TopologyId topologyId, final RibId ribId) {
        final TopologyBuilder topologyBuilder = new TopologyBuilder();
        topologyBuilder.setTopologyId(topologyId);
        topologyBuilder.setKey(new TopologyKey(topologyBuilder.getTopologyId()));
        topologyBuilder.setTopologyTypes(topologyTypes);
        topologyBuilder.addAugmentation(Topology1.class, new Topology1Builder().setRibId(ribId).build());
        return topologyBuilder.build();
    }

    private static void writeConfiguration(final ReadWriteTransaction rwTx, final KeyedInstanceIdentifier<Topology, TopologyKey> topologyIId, final Topology topology) {
        rwTx.put(LogicalDatastoreType.CONFIGURATION, topologyIId, topology, true);
    }

}