/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider.config;

import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import java.lang.reflect.Method;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyDeployer;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.TopologyReferenceSingletonService;
import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.config.rev160726.Topology1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.config.rev160726.Topology1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.osgi.framework.BundleContext;

public final class BackwardsCssTopologyProvider {

    private BackwardsCssTopologyProvider() {
        throw new UnsupportedOperationException();
    }

    public static TopologyReferenceSingletonService createBackwardsCssInstance(final TopologyTypes topologyTypes, final TopologyId topologyId, final BundleContext bundleContext,
            final KeyedInstanceIdentifier<Rib, RibKey> ribIId) {
        //map configuration to topology
        final Topology topology = createConfiguration(topologyTypes, topologyId, ribIId.getKey().getId());
        //create instance
        final WaitingServiceTracker<BgpTopologyDeployer> deployerTracker = WaitingServiceTracker.create(BgpTopologyDeployer.class, bundleContext);
        final BgpTopologyDeployer topologyDeployer = deployerTracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);
        topologyDeployer.createInstance(topology);
        //get topology service, use filter
        final WaitingServiceTracker<TopologyReference> topologyTracker = WaitingServiceTracker.create(TopologyReference.class,
                bundleContext, "(" + "topology-id" + "=" + topology.getTopologyId().getValue() + ")");
        final TopologyReference topologyService = topologyTracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);
        return Reflection.newProxy(TopologyReferenceSingletonService.class, new AbstractInvocationHandler() {
            @Override
            protected Object handleInvocation(final Object proxy, final Method method, final Object[] args) throws Throwable {
                if (method.getName().equals("close")) {
                    topologyDeployer.removeInstance(topology);
                    deployerTracker.close();
                    topologyTracker.close();
                    return null;
                }

                return method.invoke(topologyService, args);
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

}