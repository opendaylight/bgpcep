/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.state;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibStateConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateProvider;

@Singleton
// This class is thread-safe
public final class BGPStateCollectorImpl extends AbstractBGPStateConsumer implements BGPStateProvider {
    private final List<BGPRibStateConsumer> bgpRibStates = new CopyOnWriteArrayList<>();
    private final List<BGPPeerStateConsumer> bgpPeerStates = new CopyOnWriteArrayList<>();

    @Inject
    public BGPStateCollectorImpl() {
        // Exposed for DI
    }

    @Override
    public void bind(final BGPRibStateConsumer bgpState) {
        bgpRibStates.add(bgpState);
    }

    @Override
    public void bind(final BGPPeerStateConsumer bgpState) {
        bgpPeerStates.add(bgpState);
    }

    @Override
    public void unbind(final BGPRibStateConsumer bgpState) {
        bgpRibStates.remove(bgpState);
    }

    @Override
    public void unbind(final BGPPeerStateConsumer bgpState) {
        bgpPeerStates.remove(bgpState);
    }

    @Override
    List<BGPRibStateConsumer> bgpRibStates() {
        return bgpRibStates;
    }

    @Override
    List<BGPPeerStateConsumer> bgpPeerStates() {
        return bgpPeerStates;
    }
}
