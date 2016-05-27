/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.stats.rib.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpRenderState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.RIBImplRuntimeMXBean;
import org.opendaylight.protocol.bgp.rib.impl.stats.UnsignedInt32Counter;
import org.opendaylight.protocol.bgp.rib.impl.stats.peer.route.PerTableTypeRouteCounter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;

/**
 * @author Kevin Wang
 */
public class RIBImplRuntimeMXBeanImpl implements BGPRenderStats, RIBImplRuntimeMXBean {
    private final BGPRenderStats renderStats;

    public RIBImplRuntimeMXBeanImpl(@Nonnull final BgpId bgpId, @Nonnull final RibId ribId, @Nonnull final AsNumber localAs, @Nullable final ClusterIdentifier clusterId) {
        renderStats = new BGPRenderStatsImpl(bgpId, ribId, localAs, clusterId);
    }

    @Override
    public BgpRenderState getBgpRenderState() {
        return this.renderStats.getBgpRenderState();
    }

    @Override
    public PerTableTypeRouteCounter getLocRibRouteCounter() {
        return this.renderStats.getLocRibRouteCounter();
    }

    @Override
    public UnsignedInt32Counter getConfiguredPeerCounter() {
        return this.renderStats.getConfiguredPeerCounter();
    }

    @Override
    public UnsignedInt32Counter getConnectedPeerCounter() {
        return this.renderStats.getConnectedPeerCounter();
    }
}
