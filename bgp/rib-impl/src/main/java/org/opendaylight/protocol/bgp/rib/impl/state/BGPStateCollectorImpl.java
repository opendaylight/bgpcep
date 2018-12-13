/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.state;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibStateConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateProvider;

@ThreadSafe
public class BGPStateCollectorImpl implements BGPStateProvider, BGPStateConsumer {
    private final List<BGPRibStateConsumer> bgpRibStates = new CopyOnWriteArrayList<>();
    private final List<BGPPeerStateConsumer> bgpPeerStates = new CopyOnWriteArrayList<>();

    @Override
    public List<BGPRibState> getRibStats() {
        return this.bgpRibStates.stream().map(BGPRibStateConsumer::getRIBState).filter(Objects::nonNull)
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public List<BGPPeerState> getPeerStats() {
        return this.bgpPeerStates.stream().map(BGPPeerStateConsumer::getPeerState).filter(Objects::nonNull)
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public void bind(final BGPRibStateConsumer bgpState) {
        this.bgpRibStates.add(bgpState);
    }

    @Override
    public void bind(final BGPPeerStateConsumer bgpState) {
        this.bgpPeerStates.add(bgpState);
    }

    @Override
    public void unbind(final BGPRibStateConsumer bgpState) {
        this.bgpRibStates.remove(bgpState);
    }

    @Override
    public void unbind(final BGPPeerStateConsumer bgpState) {
        this.bgpPeerStates.remove(bgpState);
    }
}
