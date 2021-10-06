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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateProvider;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibStateProvider;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateProvider;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateProviderRegistry;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@MetaInfServices
@Component(immediate = true, service = {BGPStateProvider.class, BGPStateProviderRegistry.class})
public final class BGPStateCollector implements BGPStateProviderRegistry, BGPStateProvider {
    private static final Logger LOG = LoggerFactory.getLogger(BGPStateCollector.class);

    private final class BGPPeerStateProviderRegistration extends AbstractObjectRegistration<BGPPeerStateProvider> {
        BGPPeerStateProviderRegistration(final @NonNull BGPPeerStateProvider instance) {
            super(instance);
        }

        @Override
        protected void removeRegistration() {
            bgpPeerStates.remove(this);
        }
    }

    private final class BGPRibStateProviderRegistration extends AbstractObjectRegistration<BGPRibStateProvider> {
        BGPRibStateProviderRegistration(final @NonNull BGPRibStateProvider instance) {
            super(instance);
        }

        @Override
        protected void removeRegistration() {
            bgpRibStates.remove(this);
        }
    }

    private final Set<BGPPeerStateProviderRegistration> bgpPeerStates = ConcurrentHashMap.newKeySet();
    private final Set<BGPRibStateProviderRegistration> bgpRibStates = ConcurrentHashMap.newKeySet();

    @Inject
    public BGPStateCollector() {
        // Exposed for DI
    }

    @Override
    public Registration register(final BGPRibStateProvider ribStateProvider) {
        LOG.info("Registering BGPRibStateProvider");
        final var reg = new BGPRibStateProviderRegistration(ribStateProvider);
        bgpRibStates.add(reg);
        return reg;
    }

    @Override
    public Registration register(final BGPPeerStateProvider peerStateProvider) {
        LOG.info("Registering BGPPeerStateProvider");
        final var reg = new BGPPeerStateProviderRegistration(peerStateProvider);
        bgpPeerStates.add(reg);
        return reg;
    }

    @Override
    public List<BGPRibState> getRibStats() {
        return bgpRibStates.stream()
            .filter(AbstractObjectRegistration::notClosed)
            .map(reg -> reg.getInstance().getRIBState())
            .filter(Objects::nonNull)
            .collect(ImmutableList.toImmutableList());
    }

    @Override
    public List<BGPPeerState> getPeerStats() {
        return bgpPeerStates.stream()
            .filter(AbstractObjectRegistration::notClosed)
            .map(reg -> reg.getInstance().getPeerState())
            .filter(Objects::nonNull)
            .collect(ImmutableList.toImmutableList());
    }
}
