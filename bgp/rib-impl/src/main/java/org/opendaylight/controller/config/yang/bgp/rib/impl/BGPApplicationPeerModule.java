/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.protocol.bgp.rib.impl.spi.BgpDeployer;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.ProtocolKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.BGP;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Protocol1;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.osgi.framework.BundleContext;

/**
 * Application peer handler which handles translation from custom RIB into local RIB
 */
@Deprecated
public class BGPApplicationPeerModule extends org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractBGPApplicationPeerModule {

    private BundleContext bundleContext;

    public BGPApplicationPeerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BGPApplicationPeerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.bgp.rib.impl.BGPApplicationPeerModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final RIB rib = getTargetRibDependency();
        final WaitingServiceTracker<BgpDeployer> bgpDeployerTracker = WaitingServiceTracker.create(BgpDeployer.class, this.bundleContext);
        final BgpDeployer bgpDeployer = bgpDeployerTracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);
        //map configuration to OpenConfig BGP
        final Neighbor neighbor = bgpDeployer.getMappingService().fromApplicationPeer(getApplicationRibId(), getBgpPeerId());
        //write to configuration DS
        final KeyedInstanceIdentifier<Protocol, ProtocolKey> protocolIId = bgpDeployer.getInstanceIdentifier().child(Protocols.class)
            .child(Protocol.class, new ProtocolKey(BGP.class, rib.getInstanceIdentifier().getKey().getId().getValue()));
        final InstanceIdentifier<Bgp> bgpIID = protocolIId.augmentation(Protocol1.class).child(Bgp.class);
        final KeyedInstanceIdentifier<Neighbor, NeighborKey> neighborIId = protocolIId.augmentation(Protocol1.class).child(Bgp.class)
            .child(Neighbors.class).child(Neighbor.class, neighbor.getKey());
        bgpDeployer.onNeighborModified(bgpIID, neighbor, () -> bgpDeployer.writeConfiguration(neighbor, neighborIId));

        return () -> {
            bgpDeployer.onNeighborRemoved(bgpIID, neighbor);
            bgpDeployerTracker.close();
        };
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
