/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.state;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibStateConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateConsumer;

abstract class AbstractBGPStateConsumer implements BGPStateConsumer {
    @Override
    public final List<BGPRibState> getRibStats() {
        return bgpRibStates().stream()
            .map(BGPRibStateConsumer::getRIBState)
            .filter(Objects::nonNull)
            .collect(ImmutableList.toImmutableList());
    }

    @Override
    public final List<BGPPeerState> getPeerStats() {
        return bgpPeerStates().stream()
            .map(BGPPeerStateConsumer::getPeerState)
            .filter(Objects::nonNull)
            .collect(ImmutableList.toImmutableList());
    }

    abstract List<BGPRibStateConsumer> bgpRibStates();

    abstract List<BGPPeerStateConsumer> bgpPeerStates();
}
