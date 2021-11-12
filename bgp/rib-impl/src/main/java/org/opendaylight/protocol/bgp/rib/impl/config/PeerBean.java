/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import com.google.common.util.concurrent.FluentFuture;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Common interface for BgpPeer and AppPeer beans.
 */
// FIXME: this should be an abstract class and implement BGPPeerStateProvider
interface PeerBean {

    void start(RIB rib, Neighbor neighbor, InstanceIdentifier<Bgp> bgpIid, PeerGroupConfigLoader peerGroupLoader,
            BGPTableTypeRegistryConsumer tableTypeRegistry);

    // FIXME: this should return a future BGPClusterSingletonService is stopping multiple instances in one go
    void stop() throws ExecutionException, InterruptedException;

    void instantiateServiceInstance();

    FluentFuture<? extends CommitInfo> closeServiceInstance();

    Boolean containsEqualConfiguration(Neighbor neighbor);

    Neighbor getCurrentConfiguration();
}
