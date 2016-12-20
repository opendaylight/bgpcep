/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.state;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRIBState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateCollector;

@ThreadSafe
public class BGPStateCollectorImpl implements BGPStateCollector {
    @GuardedBy("this")
    private final List<BGPState> bgpStats = new ArrayList<>();

    @Override
    public synchronized List<BGPRIBState> getRibStats() {
        return ImmutableList.copyOf(this.bgpStats.stream()
            .filter(state -> state instanceof BGPRIBState)
            .map(state -> (BGPRIBState) state)
            .collect(Collectors.toList()));
    }

    @Override
    public synchronized List<BGPPeerState> getPeerStats() {
        return ImmutableList.copyOf(this.bgpStats.stream()
            .filter(state -> state instanceof BGPPeerState)
            .map(state -> (BGPPeerState) state)
            .collect(Collectors.toList()));
    }

    @Override
    public synchronized void bind(final BGPState bgpState) {
        this.bgpStats.add(bgpState);
    }

    @Override
    public synchronized void unbind(final BGPState bgpState) {
        this.bgpStats.remove(bgpState);
    }
}
