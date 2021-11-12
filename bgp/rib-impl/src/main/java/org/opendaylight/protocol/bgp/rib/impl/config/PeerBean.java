/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateProvider;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Common interface for BgpPeer and AppPeer beans.
 */
abstract class PeerBean implements BGPPeerStateProvider {

    abstract void start(RIB rib, Neighbor neighbor, InstanceIdentifier<Bgp> bgpIid,
        PeerGroupConfigLoader peerGroupLoader, BGPTableTypeRegistryConsumer tableTypeRegistry);

    abstract ListenableFuture<?> stop();

    abstract void instantiateServiceInstance();

    abstract ListenableFuture<?> closeServiceInstance();

    abstract boolean containsEqualConfiguration(Neighbor neighbor);

    abstract Neighbor getCurrentConfiguration();
}
