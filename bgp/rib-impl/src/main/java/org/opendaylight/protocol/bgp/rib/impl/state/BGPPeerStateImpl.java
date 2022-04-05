/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.state;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.rib.DefaultRibReference;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPMessagesListener;
import org.opendaylight.protocol.bgp.rib.impl.state.peer.PrefixesInstalledCounters;
import org.opendaylight.protocol.bgp.rib.impl.state.peer.PrefixesReceivedCounters;
import org.opendaylight.protocol.bgp.rib.impl.state.peer.PrefixesSentCounters;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPAfiSafiState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPErrorHandlingState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPGracelfulRestartState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPLlGracelfulRestartState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerMessagesState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateProvider;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpAfiSafiGracefulRestartState.Mode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;

public abstract class BGPPeerStateImpl extends DefaultRibReference implements BGPPeerState, BGPAfiSafiState,
        BGPGracelfulRestartState, BGPLlGracelfulRestartState,BGPErrorHandlingState, BGPPeerMessagesState,
        BGPPeerStateProvider, BGPMessagesListener {
    private static final long NONE = 0L;
    private final IpAddressNoZone neighborAddress;
    private final Set<TablesKey> afiSafisAdvertized;
    private final Set<TablesKey> afiSafisGracefulAdvertized;
    private final Set<TablesKey> afiSafisGracefulReceived = new HashSet<>();
    private final Map<TablesKey, Integer> afiSafisLlGracefulAdvertised;
    private final Map<TablesKey, Integer> afiSafisLlGracefulReceived = new HashMap<>();
    private final LongAdder updateSentCounter = new LongAdder();
    private final LongAdder notificationSentCounter = new LongAdder();
    private final LongAdder updateReceivedCounter = new LongAdder();
    private final LongAdder notificationReceivedCounter = new LongAdder();
    private final LongAdder erroneousUpdate = new LongAdder();
    private final String groupId;
    private final AtomicBoolean active = new AtomicBoolean(false);

    @GuardedBy("this")
    private final Map<TablesKey, PrefixesSentCounters> prefixesSent = new HashMap<>();
    @GuardedBy("this")
    private PrefixesReceivedCounters prefixesReceived;
    @GuardedBy("this")
    private PrefixesInstalledCounters prefixesInstalled;
    @GuardedBy("this")
    private boolean localRestarting;
    @GuardedBy("this")
    private int peerRestartTime;
    @GuardedBy("this")
    private boolean peerRestarting;

    public BGPPeerStateImpl(final @NonNull KeyedInstanceIdentifier<Rib, RibKey> instanceIdentifier,
            final @Nullable String groupId, final @NonNull IpAddressNoZone neighborAddress,
            final @NonNull Set<TablesKey> afiSafisAdvertized,
            final @NonNull Set<TablesKey> afiSafisGracefulAdvertized,
            final @NonNull Map<TablesKey, Integer> afiSafisLlGracefulAdvertized) {
        super(instanceIdentifier);
        this.neighborAddress = requireNonNull(neighborAddress);
        this.groupId = groupId;
        this.afiSafisAdvertized = requireNonNull(afiSafisAdvertized);
        this.afiSafisGracefulAdvertized = requireNonNull(afiSafisGracefulAdvertized);
        afiSafisLlGracefulAdvertised = requireNonNull(afiSafisLlGracefulAdvertized);
    }

    @Override
    public final String getGroupId() {
        return groupId;
    }

    @Override
    public final IpAddressNoZone getNeighborAddress() {
        return neighborAddress;
    }

    @Override
    public final synchronized long getTotalPrefixes() {
        if (prefixesInstalled == null) {
            return NONE;
        }
        return prefixesInstalled.getTotalPrefixesInstalled();
    }

    @Override
    public final BGPPeerMessagesState getBGPPeerMessagesState() {
        return this;
    }

    @Override
    public final BGPGracelfulRestartState getBGPGracelfulRestart() {
        return this;
    }

    @Override
    public final synchronized boolean isAfiSafiSupported(final TablesKey tablesKey) {
        return prefixesReceived != null && prefixesReceived.isSupported(tablesKey)
                && afiSafisAdvertized.contains(tablesKey);
    }

    @Override
    public final synchronized long getPrefixesInstalledCount(final TablesKey tablesKey) {
        if (prefixesInstalled == null) {
            return NONE;
        }
        return prefixesInstalled.getPrefixedInstalledCount(tablesKey);
    }

    @Override
    public final synchronized long getPrefixesSentCount(final TablesKey tablesKey) {
        if (prefixesSent == null) {
            return 0;
        }
        final PrefixesSentCounters counter = prefixesSent.get(tablesKey);
        if (counter == null) {
            return NONE;
        }
        return counter.getPrefixesSentCount();
    }

    @Override
    public final synchronized long getPrefixesReceivedCount(final TablesKey tablesKey) {
        if (prefixesReceived == null) {
            return NONE;
        }
        return prefixesReceived.getPrefixedReceivedCount(tablesKey);
    }

    @Override
    public final Set<TablesKey> getAfiSafisAdvertized() {
        return ImmutableSet.copyOf(afiSafisAdvertized);
    }

    @Override
    public final synchronized Set<TablesKey> getAfiSafisReceived() {
        if (prefixesReceived == null) {
            return Collections.emptySet();
        }
        return prefixesReceived.getTableKeys();
    }

    @Override
    public final boolean isGracefulRestartAdvertized(final TablesKey tablesKey) {
        return afiSafisGracefulAdvertized.contains(tablesKey);
    }

    @Override
    public final boolean isGracefulRestartReceived(final TablesKey tablesKey) {
        return afiSafisGracefulReceived.contains(tablesKey);
    }

    @Override
    public final synchronized boolean isLocalRestarting() {
        return localRestarting;
    }

    @Override
    public final synchronized int getPeerRestartTime() {
        return peerRestartTime;
    }

    @Override
    public final synchronized boolean isPeerRestarting() {
        return peerRestarting;
    }

    public final synchronized void setAfiSafiGracefulRestartState(final int newPeerRestartTime,
            final boolean newPeerRestarting, final boolean newLocalRestarting) {
        peerRestartTime = newPeerRestartTime;
        peerRestarting = newPeerRestarting;
        localRestarting = newLocalRestarting;
    }

    protected final synchronized void setAdvertizedGracefulRestartTableTypes(final List<TablesKey> receivedGraceful) {
        afiSafisGracefulReceived.clear();
        afiSafisGracefulReceived.addAll(receivedGraceful);
    }

    protected final synchronized void registerPrefixesSentCounter(final TablesKey tablesKey,
        final PrefixesSentCounters prefixesSentCounter) {
        prefixesSent.put(tablesKey, prefixesSentCounter);
    }

    protected final synchronized void registerPrefixesCounters(
            final @NonNull PrefixesReceivedCounters newPrefixesReceived,
            final @NonNull PrefixesInstalledCounters newPrefixesInstalled) {
        prefixesReceived = newPrefixesReceived;
        prefixesInstalled = newPrefixesInstalled;
    }

    protected final synchronized void resetState() {
        localRestarting = false;
        peerRestartTime = 0;
        peerRestarting = false;
    }

    protected final synchronized void setRestartingState() {
        peerRestarting = true;
    }

    protected final synchronized void setLocalRestartingState(final boolean restarting) {
        localRestarting = restarting;
    }

    @Override
    public final BGPPeerState getPeerState() {
        return this;
    }

    @Override
    public final long getErroneousUpdateReceivedCount() {
        //FIXME BUG-4979
        return erroneousUpdate.longValue();
    }

    @Override
    public final long getUpdateMessagesSentCount() {
        return updateSentCounter.longValue();
    }

    @Override
    public final long getNotificationMessagesSentCount() {
        return notificationSentCounter.longValue();
    }

    @Override
    public final long getUpdateMessagesReceivedCount() {
        return updateReceivedCounter.longValue();
    }

    @Override
    public final long getNotificationMessagesReceivedCount() {
        return notificationReceivedCounter.longValue();
    }

    @Override
    public final void messageSent(final Notification<?> msg) {
        if (msg instanceof Notify) {
            notificationSentCounter.increment();
        } else if (msg instanceof Update) {
            updateSentCounter.increment();
        }
    }

    @Override
    public final void messageReceived(final Notification<?> msg) {
        if (msg instanceof Notify) {
            notificationReceivedCounter.increment();
        } else if (msg instanceof Update) {
            updateReceivedCounter.increment();
        }
    }

    @Override
    public final boolean isActive() {
        return active.get();
    }

    protected final void setActive(final boolean active) {
        this.active.set(active);
    }

    @Override
    public final synchronized Mode getMode() {
        if (afiSafisGracefulAdvertized.isEmpty()) {
            return Mode.HELPERONLY;
        }
        if (afiSafisGracefulReceived.isEmpty()) {
            return Mode.REMOTEHELPER;
        }
        return Mode.BILATERAL;
    }

    public final synchronized void setAdvertizedLlGracefulRestartTableTypes(
            final Map<TablesKey, Integer> afiSafiReceived) {
        afiSafisLlGracefulReceived.clear();
        afiSafisLlGracefulReceived.putAll(afiSafiReceived);
    }

    @Override
    public final synchronized boolean isLlGracefulRestartAdvertised(final TablesKey tablesKey) {
        return afiSafisLlGracefulAdvertised.containsKey(tablesKey);
    }

    @Override
    public final synchronized boolean isLlGracefulRestartReceived(final TablesKey tablesKey) {
        return afiSafisLlGracefulReceived.containsKey(tablesKey);
    }

    @Override
    public final synchronized int getLlGracefulRestartTimer(final TablesKey tablesKey) {
        final int timerAdvertised = isLlGracefulRestartAdvertised(tablesKey)
                ? afiSafisLlGracefulAdvertised.get(tablesKey) : 0;
        final int timerReceived = isLlGracefulRestartReceived(tablesKey)
                ? afiSafisLlGracefulReceived.get(tablesKey) : 0;
        return Integer.min(timerAdvertised, timerReceived);
    }
}
