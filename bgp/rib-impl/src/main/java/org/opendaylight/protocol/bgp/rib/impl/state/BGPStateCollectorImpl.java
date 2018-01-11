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
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRIBState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRIBStateConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateProvider;

@ThreadSafe
public class BGPStateCollectorImpl implements BGPStateProvider, BGPStateConsumer {
    @GuardedBy("this")
    private final List<BGPRIBStateConsumer> bgpRibStates = new ArrayList<>();
    @GuardedBy("this")
    private final List<BGPPeerStateConsumer> bgpPeerStates = new ArrayList<>();

    @Override
    public List<BGPRIBState> getRibStats() {
        synchronized (this.bgpRibStates) {
            return ImmutableList.copyOf(this.bgpRibStates
                    .stream()
                    .map(BGPRIBStateConsumer::getRIBState)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        }
    }

    @Override
    public List<BGPPeerState> getPeerStats() {
        synchronized (this.bgpPeerStates) {
            return ImmutableList.copyOf(this.bgpPeerStates
                    .stream()
                    .map(BGPPeerStateConsumer::getPeerState)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        }
    }

    @Override
    public void bind(final BGPRIBStateConsumer bgpState) {
        if (bgpState == null) {
            return;
        }
        synchronized (this.bgpRibStates) {
            this.bgpRibStates.add(bgpState);
        }
    }

    @Override
    public void unbind(final BGPRIBStateConsumer bgpState) {
        if (bgpState == null) {
            return;
        }
        synchronized (this.bgpRibStates) {
            this.bgpRibStates.remove(bgpState);
        }
    }

    @Override
    public void bind(final BGPPeerStateConsumer bgpState) {
        if (bgpState == null) {
            return;
        }
        synchronized (this.bgpPeerStates) {
            this.bgpPeerStates.add(bgpState);
        }
    }

    @Override
    public void unbind(final BGPPeerStateConsumer bgpState) {
        if (bgpState == null) {
            return;
        }
        synchronized (this.bgpPeerStates) {
            this.bgpPeerStates.remove(bgpState);
        }
    }
}
