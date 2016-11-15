/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.state.impl.neighbor;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.state.spi.NeighborStateUtil;
import org.opendaylight.protocol.bgp.state.spi.counters.BGPCountersMessagesTypesCommon;
import org.opendaylight.protocol.bgp.state.spi.counters.UnsignedInt32Counter;
import org.opendaylight.protocol.bgp.state.spi.state.BGPNeighborState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpAfiSafiGracefulRestartState.Mode;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpNeighborState.SessionState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.BgpCapability;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.BgpNeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborErrorHandlingStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborGracefulRestartStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTimersStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTransportStateAugmentation;

public final class BGPNeighborStateImpl implements BGPNeighborState {
    private final BGPCountersMessagesTypesCommon messagesSentCounter;
    private final BGPCountersMessagesTypesCommon messagesReceivedCounter;
    private final UnsignedInt32Counter erroneousUpdate;
    private final Stopwatch sessionStopwatch;
    private final IpAddress neighborAddress;
    private final Set<Class<? extends AfiSafiType>> afiSafisAdvertized;
    private final Set<Class<? extends AfiSafiType>> afiSafisGracefulAdvertized;
    private final Set<Class<? extends AfiSafiType>> afiSafisGracefulReceived = new HashSet<>();
    @GuardedBy("this")
    private final Map<Class<? extends AfiSafiType>, UnsignedInt32Counter> prefixesReceived = new HashMap<>();
    @GuardedBy("this")
    private final Map<Class<? extends AfiSafiType>, UnsignedInt32Counter> prefixesInstalled = new HashMap<>();
    @GuardedBy("this")
    private final Map<Class<? extends AfiSafiType>, UnsignedInt32Counter> prefixesSent = new HashMap<>();
    @GuardedBy("this")
    private SessionState sessionState;
    private List<Class<? extends BgpCapability>> supportedCapabilities = new ArrayList<>();
    private int holdTimerValue;
    private IpAddress remoteAddress;
    private int remotePort;
    private int localPort;
    private Boolean localRestarting;
    private Integer peerRestartTime;
    private Mode mode;
    private Boolean peerRestarting;

    public BGPNeighborStateImpl(@Nonnull final IpAddress neighborAddress,
        final Set<Class<? extends AfiSafiType>> afiSafisAdvertized,
        final Set<Class<? extends AfiSafiType>> afiSafisGracefulAdvertized) {
        this.neighborAddress = Preconditions.checkNotNull(neighborAddress);
        this.sessionState = SessionState.ACTIVE;
        final String address = this.neighborAddress.getIpv4Address().getValue();
        this.messagesSentCounter = new BGPCountersMessagesTypesCommon(address, "Sent");
        this.messagesReceivedCounter = new BGPCountersMessagesTypesCommon(address, "Received");
        this.erroneousUpdate = new UnsignedInt32Counter("Total Erroneous Update received on neighbor " + address + " ");
        this.afiSafisAdvertized = Preconditions.checkNotNull(afiSafisAdvertized);
        this.afiSafisGracefulAdvertized = Preconditions.checkNotNull(afiSafisGracefulAdvertized);
        this.sessionStopwatch = Stopwatch.createUnstarted();
    }

    @Override
    public IpAddress getNeighborAddress() {
        return this.neighborAddress;
    }

    @Override
    public NeighborStateAugmentation getCapabilitiesState() {
        return NeighborStateUtil.buildCapabilityState(this.supportedCapabilities, this.sessionState);
    }

    @Override
    public BgpNeighborStateAugmentation getMessagesState() {
        return NeighborStateUtil.buildMessageState(this.messagesReceivedCounter, this.messagesSentCounter);
    }

    @Override
    public NeighborTimersStateAugmentation getTimersState() {
        return NeighborStateUtil.buildTimerState(this.holdTimerValue, this.sessionStopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Override
    public NeighborTransportStateAugmentation getTransportState() {
        return NeighborStateUtil.buildTransportState(this.localPort, this.remoteAddress, this.remotePort);
    }

    @Override
    public NeighborErrorHandlingStateAugmentation getErrorHandlingState() {
        return NeighborStateUtil.buildErrorHandlingState(this.erroneousUpdate.getCount());
    }

    @Override
    public NeighborGracefulRestartStateAugmentation getGracefulRestartState() {
        return NeighborStateUtil.buildGracefulRestartState(this.localRestarting, this.peerRestartTime, this.mode,
            this.peerRestarting);
    }

    @Override
    public synchronized List<AfiSafi> getAfisSafisState() {
        return NeighborStateUtil.buildAfisSafisState(
            this.prefixesSent.keySet(),
            this.afiSafisAdvertized,
            this.afiSafisGracefulAdvertized,
            this.afiSafisGracefulReceived,
            this.prefixesInstalled,
            this.prefixesReceived,
            this.prefixesSent);
    }

    @Override
    public synchronized long getTotalPrefixes() {
        return this.prefixesInstalled.values().stream().mapToLong(UnsignedInt32Counter::getCount).sum();
    }

    @Override
    public void setCapabilities(final int holdTimerValue, final int localPort, final IpAddress remoteAddress,
        final int remotePort, final boolean addPath, final boolean asn32, final boolean gracefulRestart, final boolean multiProtocol,
        final boolean routerRefresh) {
        this.holdTimerValue = holdTimerValue;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.localPort = localPort;

        this.supportedCapabilities = NeighborStateUtil.buildSupportedCapabilities(addPath, asn32, gracefulRestart,
            multiProtocol, routerRefresh);
    }

    @Override
    public void setAfiSafiGracefulRestartState(final int peerRestartTime, final boolean peerRestarting, final boolean localRestarting,
        final Mode mode) {
        this.peerRestartTime = peerRestartTime;
        this.peerRestarting = peerRestarting;
        this.localRestarting = localRestarting;
        this.mode = mode;
    }

    @Override
    public synchronized void setActiveAfiSafi(final Set<Class<? extends AfiSafiType>> receivedAfiSafis,
        final Set<Class<? extends AfiSafiType>> receivedAfiSafisGraceful) {
        this.afiSafisGracefulReceived.addAll(afiSafisGracefulReceived);
        receivedAfiSafis.forEach(tablesKey -> {
            this.prefixesInstalled.put(tablesKey, new UnsignedInt32Counter("Total Prefixes installed on neighborAddress " + this.neighborAddress + " AFI-SAFI" + tablesKey));
            this.prefixesReceived.put(tablesKey, new UnsignedInt32Counter("Total Prefixes received by neighborAddress " + this.neighborAddress + " AFI-SAFI" + tablesKey));
            this.prefixesSent.put(tablesKey, new UnsignedInt32Counter("Total Prefixes sent by neighborAddress " + this.neighborAddress + " AFI-SAFI" + tablesKey));
        });
    }

    @Override
    public synchronized void increasePrefixesInstalled(final Class<? extends AfiSafiType> afiSafi) {
        this.prefixesInstalled.get(afiSafi).increaseCount();
    }

    @Override
    public synchronized void decreasePrefixesInstalled(final Class<? extends AfiSafiType> afiSafi) {
        this.prefixesInstalled.get(afiSafi).decreaseCount();
    }

    @Override
    public synchronized void increasePrefixesSent(final Class<? extends AfiSafiType> afiSafi) {
        this.prefixesSent.get(afiSafi).increaseCount();
    }

    @Override
    public synchronized void increasePrefixesReceived(final Class<? extends AfiSafiType> afiSafi, final long numberOfPrefixesInstalled) {
        this.prefixesReceived.get(afiSafi).increaseCount(numberOfPrefixesInstalled);
    }

    @Override
    public void increaseErroneousUpdateReceived() {
        this.erroneousUpdate.increaseCount();
    }

    @Override
    public void increaseNotificationSent() {
        this.messagesSentCounter.increaseNotification();
    }

    @Override
    public void increaseUpdateSent() {
        this.messagesSentCounter.increaseUpdate();
    }

    @Override
    public void increaseUpdateReceived() {
        this.messagesReceivedCounter.increaseUpdate();
    }

    @Override
    public void increaseNotificationReceived() {
        this.messagesReceivedCounter.increaseNotification();
    }

    @Override
    public synchronized void setState(final SessionState sessionState) {
        if (sessionState == SessionState.ESTABLISHED || sessionState == SessionState.IDLE) {
            this.sessionStopwatch.reset();
            this.sessionStopwatch.start();
        }
        this.sessionState = sessionState;
    }
}
