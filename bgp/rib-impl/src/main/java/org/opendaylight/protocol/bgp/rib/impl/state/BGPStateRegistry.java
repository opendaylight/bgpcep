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
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateProvider;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibStateProvider;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateProvider;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateProviderConsumer;
import org.osgi.service.component.annotations.Component;

@Singleton
@MetaInfServices
@Component(immediate = true, service = {BGPStateProvider.class, BGPStateProviderConsumer.class})
public final class BGPStateRegistry implements BGPStateProviderConsumer, BGPStateProvider {
    private final List<BGPRibStateProvider> bgpRibStates = new CopyOnWriteArrayList<>();
    private final List<BGPPeerStateProvider> bgpPeerStates = new CopyOnWriteArrayList<>();

    @Inject
    public BGPStateRegistry() {
        // Exposed for DI
    }

    @Override
    public void bind(final BGPRibStateProvider ribStateProvider) {
        bgpRibStates.add(ribStateProvider);
    }

    @Override
    public void bind(final BGPPeerStateProvider peerStateProvider) {
        bgpPeerStates.add(peerStateProvider);
    }

    @Override
    public void unbind(final BGPRibStateProvider ribStateProvider) {
        bgpRibStates.remove(ribStateProvider);
    }

    @Override
    public void unbind(final BGPPeerStateProvider peerStateProvider) {
        bgpPeerStates.remove(peerStateProvider);
    }


    @Override
    public List<BGPRibState> getRibStats() {
        return bgpRibStates.stream()
                .map(BGPRibStateProvider::getRIBState)
                .filter(Objects::nonNull)
                .collect(ImmutableList.toImmutableList());
    }

    @Override
    public List<BGPPeerState> getPeerStats() {
        return bgpPeerStates.stream()
                .map(BGPPeerStateProvider::getPeerState)
                .filter(Objects::nonNull)
                .collect(ImmutableList.toImmutableList());
    }
}
