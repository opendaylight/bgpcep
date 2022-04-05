/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.state;

import com.google.common.base.Stopwatch;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.protocol.bgp.rib.impl.StrictBGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPMessagesListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionStateListener;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPSessionState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTimersState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTransportState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapability;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.Uint16;

// This class is thread-safe
public final class BGPSessionStateImpl implements BGPSessionState, BGPTimersState, BGPTransportState,
    BGPSessionStateListener {
    private static final PortNumber NON_DEFINED_PORT = new PortNumber(Uint16.ZERO);
    private final Stopwatch sessionStopwatch;
    private int holdTimerValue;
    private IpAddressNoZone remoteAddress;
    private PortNumber remotePort = NON_DEFINED_PORT;
    private PortNumber localPort = NON_DEFINED_PORT;
    @GuardedBy("this")
    private boolean addPathCapability;
    @GuardedBy("this")
    private boolean asn32Capability;
    @GuardedBy("this")
    private boolean gracefulRestartCapability;
    @GuardedBy("this")
    private boolean multiProtocolCapability;
    @GuardedBy("this")
    private boolean routerRefreshCapability;
    @GuardedBy("this")
    private State sessionState;
    @GuardedBy("this")
    private BGPMessagesListener messagesListenerCounter;

    public BGPSessionStateImpl() {
        sessionState = State.OPEN_CONFIRM;
        sessionStopwatch = Stopwatch.createUnstarted();
    }

    @Override
    public synchronized void messageSent(final Notification<?> msg) {
        if (messagesListenerCounter != null) {
            messagesListenerCounter.messageSent(msg);
        }
    }

    @Override
    public synchronized void messageReceived(final Notification<?> msg) {
        if (messagesListenerCounter != null) {
            messagesListenerCounter.messageReceived(msg);
        }
    }

    @Override
    public synchronized void advertizeCapabilities(final int newHoldTimerValue, final SocketAddress newRemoteAddress,
        final SocketAddress localAddress, final Set<BgpTableType> tableTypes, final List<BgpParameters> bgpParameters) {
        if (bgpParameters != null) {
            for (final BgpParameters parameters : bgpParameters) {
                for (final OptionalCapabilities optionalCapabilities : parameters.nonnullOptionalCapabilities()) {
                    final CParameters cParam = optionalCapabilities.getCParameters();
                    final CParameters1 capabilities = cParam.augmentation(CParameters1.class);
                    if (capabilities != null) {
                        final MultiprotocolCapability mc = capabilities.getMultiprotocolCapability();
                        if (mc != null) {
                            multiProtocolCapability = true;
                        }
                        if (capabilities.getGracefulRestartCapability() != null) {
                            gracefulRestartCapability = true;
                        }
                        if (capabilities.getAddPathCapability() != null) {
                            addPathCapability = true;
                        }
                        if (capabilities.getRouteRefreshCapability() != null) {
                            routerRefreshCapability = true;
                        }
                    }
                    if (cParam.getAs4BytesCapability() != null) {
                        asn32Capability = true;
                    }
                }
            }
        }

        holdTimerValue = newHoldTimerValue;
        remoteAddress = StrictBGPPeerRegistry.getIpAddress(newRemoteAddress);
        remotePort = new PortNumber(Uint16.valueOf(((InetSocketAddress) newRemoteAddress).getPort()));
        localPort = new PortNumber(Uint16.valueOf(((InetSocketAddress) localAddress).getPort()));
    }

    @Override
    public synchronized State getSessionState() {
        return sessionState;
    }

    @Override
    public synchronized void setSessionState(final State state) {
        if (state == State.IDLE) {
            sessionStopwatch.reset();
        } else if (state == State.UP) {
            sessionStopwatch.start();
        }
        sessionState = state;
    }

    @Override
    public synchronized boolean isAddPathCapabilitySupported() {
        return addPathCapability;
    }

    @Override
    public synchronized boolean isAsn32CapabilitySupported() {
        return asn32Capability;
    }

    @Override
    public synchronized boolean isGracefulRestartCapabilitySupported() {
        return gracefulRestartCapability;
    }

    @Override
    public synchronized boolean isMultiProtocolCapabilitySupported() {
        return multiProtocolCapability;
    }

    @Override
    public synchronized boolean isRouterRefreshCapabilitySupported() {
        return routerRefreshCapability;
    }

    @Override
    public synchronized PortNumber getLocalPort() {
        return localPort;
    }

    @Override
    public synchronized IpAddressNoZone getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public synchronized PortNumber getRemotePort() {
        return remotePort;
    }

    @Override
    public synchronized long getNegotiatedHoldTime() {
        return holdTimerValue;
    }

    @Override
    public synchronized long getUpTime() {
        return sessionStopwatch.elapsed(TimeUnit.MILLISECONDS);
    }

    public synchronized void registerMessagesCounter(final BGPMessagesListener bgpMessagesListener) {
        messagesListenerCounter = bgpMessagesListener;
    }
}
