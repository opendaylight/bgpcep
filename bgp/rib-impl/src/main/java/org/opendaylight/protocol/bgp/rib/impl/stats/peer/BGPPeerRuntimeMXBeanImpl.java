/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.stats.peer;

import org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerRuntimeMXBean;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpPeerState;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BgpSessionState;
import org.opendaylight.protocol.bgp.rib.impl.stats.UnsignedInt32Counter;
import org.opendaylight.protocol.bgp.rib.impl.stats.peer.route.PerTableTypeRouteCounter;

/**
 * @author Kevin Wang
 */
public final class BGPPeerRuntimeMXBeanImpl implements BGPPeerStats, BGPSessionStats, BGPPeerRuntimeMXBean {
    private static BGPPeerRuntimeMXBeanImpl instance = null;

    private BGPPeerRuntimeMXBeanImpl() {

    }

    public static BGPPeerRuntimeMXBeanImpl getInstance() {
        if (null == instance) {
            instance = new BGPPeerRuntimeMXBeanImpl();
        }
        return instance;
    }

    @Override
    public BgpPeerState getBgpPeerState() {
        return null;
    }

    @Override
    public PerTableTypeRouteCounter getAdjRibInRouteCounters() {
        return null;
    }

    @Override
    public PerTableTypeRouteCounter getAdjRibOutRouteCounters() {
        return null;
    }

    @Override
    public PerTableTypeRouteCounter getEffectiveRibInRouteCounters() {
        return null;
    }

    @Override
    public void resetStats() {

    }

    @Override
    public void resetSession() {

    }

    @Override
    public UnsignedInt32Counter getSessionEstablishedCounter() {
        return null;
    }

    @Override
    public BgpSessionState getBgpSessionState() {
        return null;
    }

    @Override
    public void resetBgpSessionStats() {

    }
}
