/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.state;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.rib.DefaultRibReference;
import org.opendaylight.protocol.bgp.rib.impl.state.peer.PrefixesInstalledCounters;
import org.opendaylight.protocol.bgp.rib.impl.state.peer.PrefixesReceivedCounters;
import org.opendaylight.protocol.bgp.rib.impl.state.peer.PrefixesSentCounters;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPAfiSafiState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPGracelfulRestartState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateConsumer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public abstract class BGPPeerStateImpl extends DefaultRibReference implements BGPPeerState,
    BGPAfiSafiState, BGPGracelfulRestartState, BGPPeerStateConsumer {
    private static final long NONE = 0L;
    private final IpAddress neighborAddress;
    private final Set<TablesKey> afiSafisAdvertized;
    private final Set<TablesKey> afiSafisGracefulAdvertized;
    private final Set<TablesKey> afiSafisGracefulReceived = new HashSet<>();

    @GuardedBy("this")
    private final Map<TablesKey, PrefixesSentCounters> prefixesSent = new HashMap<>();
    private final String groupId;
    private PrefixesReceivedCounters prefixesReceived;
    private PrefixesInstalledCounters prefixesInstalled;
    private boolean localRestarting;
    private int peerRestartTime;
    private boolean peerRestarting;

    public BGPPeerStateImpl(@Nonnull final KeyedInstanceIdentifier<Rib, RibKey> instanceIdentifier,
        @Nullable final String groupId, @Nonnull final IpAddress neighborAddress,
        @Nonnull final Set<TablesKey> afiSafisAdvertized,
        @Nonnull final Set<TablesKey> afiSafisGracefulAdvertized) {
        super(instanceIdentifier);
        this.neighborAddress = Preconditions.checkNotNull(neighborAddress);
        this.groupId = groupId;
        this.afiSafisAdvertized = Preconditions.checkNotNull(afiSafisAdvertized);
        this.afiSafisGracefulAdvertized = Preconditions.checkNotNull(afiSafisGracefulAdvertized);
    }

    @Override
    public String getGroupId() {
        return this.groupId;
    }

    @Override
    public final IpAddress getNeighborAddress() {
        return this.neighborAddress;
    }

    @Override
    public final synchronized long getTotalPrefixes() {
        if (this.prefixesInstalled == null) {
            return NONE;
        }
        return this.prefixesInstalled.getTotalPrefixesInstalled();
    }

    @Override
    public BGPGracelfulRestartState getBGPGracelfulRestart() {
        return this;
    }

    @Override
    public final boolean isAfiSafiSupported(final TablesKey tablesKey) {
        return this.prefixesReceived != null && this.prefixesReceived.isSupported(tablesKey) &&
            this.afiSafisAdvertized.contains(tablesKey);
    }

    @Override
    public final long getPrefixesInstalledCount(final TablesKey tablesKey) {
        if (this.prefixesInstalled == null) {
            return NONE;
        }
        return this.prefixesInstalled.getPrefixedInstalledCount(tablesKey);
    }

    @Override
    public synchronized final long getPrefixesSentCount(@Nonnull final TablesKey tablesKey) {
        if (this.prefixesSent == null) {
            return 0;
        }
        final PrefixesSentCounters counter = this.prefixesSent.get(tablesKey);
        if (counter == null) {
            return NONE;
        }
        return counter.getPrefixesSentCount();
    }

    @Override
    public final long getPrefixesReceivedCount(final TablesKey tablesKey) {
        if (this.prefixesReceived == null) {
            return NONE;
        }
        return this.prefixesReceived.getPrefixedReceivedCount(tablesKey);
    }

    @Override
    public final Set<TablesKey> getAfiSafisAdvertized() {
        return ImmutableSet.copyOf(this.afiSafisAdvertized);
    }

    @Override
    public final Set<TablesKey> getAfiSafisReceived() {
        if (this.prefixesReceived == null) {
            return Collections.emptySet();
        }
        return this.prefixesReceived.getTableKeys();
    }

    @Override
    public final boolean isGracefulRestartAdvertized(final TablesKey tablesKey) {
        return this.afiSafisGracefulAdvertized.contains(tablesKey);
    }

    @Override
    public final boolean isGracefulRestartReceived(final TablesKey tablesKey) {
        return this.afiSafisGracefulReceived.contains(tablesKey);
    }

    @Override
    public final boolean isLocalRestarting() {
        return this.localRestarting;
    }

    @Override
    public final int getPeerRestartTime() {
        return this.peerRestartTime;
    }

    @Override
    public final boolean isPeerRestarting() {
        return this.peerRestarting;
    }

    public final void setAfiSafiGracefulRestartState(final int peerRestartTime, final boolean peerRestarting,
        final boolean localRestarting) {
        this.peerRestartTime = peerRestartTime;
        this.peerRestarting = peerRestarting;
        this.localRestarting = localRestarting;
    }

    protected final synchronized void setAdvertizedGracefulRestartTableTypes(final List<TablesKey> receivedGraceful) {
        this.afiSafisGracefulReceived.addAll(receivedGraceful);
    }

    protected synchronized final void registerPrefixesSentCounter(final TablesKey tablesKey,
        final PrefixesSentCounters prefixesSentCounter) {
        this.prefixesSent.put(tablesKey, prefixesSentCounter);
    }

    protected final void registerPrefixesCounters(@Nonnull final PrefixesReceivedCounters prefixesReceived,
        @Nonnull final PrefixesInstalledCounters prefixesInstalled) {
        this.prefixesReceived = prefixesReceived;
        this.prefixesInstalled = prefixesInstalled;
    }

    protected final void resetState() {
        this.localRestarting = false;
        this.peerRestartTime = 0;
        this.peerRestarting = false;
    }

    @Override
    public BGPPeerState getPeerState() {
        return this;
    }
}
