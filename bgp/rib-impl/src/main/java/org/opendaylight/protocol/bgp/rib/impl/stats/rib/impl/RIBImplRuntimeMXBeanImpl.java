/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.stats.rib.impl;

import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpRenderState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRIBState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;

/**
 * @author Kevin Wang
 */
public class RIBImplRuntimeMXBeanImpl implements BGPRenderStats {
    private final BGPRenderStats renderStats;

    public RIBImplRuntimeMXBeanImpl(@Nonnull final BgpId bgpId, @Nonnull final RibId ribId,
        @Nonnull final AsNumber localAs, @Nullable final ClusterIdentifier clusterId,
        @Nonnull final BGPRIBState globalState,
        @Nonnull final Set<TablesKey> tablesKeys) {
        this.renderStats = new BGPRenderStatsImpl(bgpId, ribId, localAs, clusterId, globalState, tablesKeys);
    }

    @Override
    public BgpRenderState getBgpRenderState() {
        return this.renderStats.getBgpRenderState();
    }

    @Override
    public LongAdder getConfiguredPeerCounter() {
        return this.renderStats.getConfiguredPeerCounter();
    }

    @Override
    public LongAdder getConnectedPeerCounter() {
        return this.renderStats.getConnectedPeerCounter();
    }
}
