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
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.rib.impl.StrictBGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPMessagesListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionStateListener;
import org.opendaylight.protocol.bgp.rib.spi.State;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPSessionState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTimersState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPTransportState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.MultiprotocolCapability;
import org.opendaylight.yangtools.yang.binding.Notification;

public final class BGPSessionStateImpl implements BGPSessionState, BGPTimersState, BGPTransportState,
    BGPSessionStateListener {
    private static final PortNumber NON_DEFINED_PORT = new PortNumber(0);
    private final Stopwatch sessionStopwatch;
    @GuardedBy("this")
    private State sessionState;
    private int holdTimerValue;
    private IpAddress remoteAddress;
    private PortNumber remotePort = NON_DEFINED_PORT;
    private PortNumber localPort = NON_DEFINED_PORT;
    private boolean addPathCapability;
    private boolean asn32Capability;
    private boolean gracefulRestartCapability;
    private boolean multiProtocolCapability;
    private boolean routerRefreshCapability;
    private BGPMessagesListener messagesListenerCounter;

    public BGPSessionStateImpl() {
        this.sessionState = State.OPEN_CONFIRM;
        this.sessionStopwatch = Stopwatch.createUnstarted();
    }

    @Override
    public synchronized void messageSent(final Notification msg) {
        if (this.messagesListenerCounter != null) {
            this.messagesListenerCounter.messageSent(msg);
        }
    }

    @Override
    public synchronized void messageReceived(final Notification msg) {
        if (this.messagesListenerCounter != null) {
            this.messagesListenerCounter.messageReceived(msg);
        }
    }

    @Override
    public void advertizeCapabilities(final int holdTimerValue, final SocketAddress remoteAddress,
        final SocketAddress localAddress, final Set<BgpTableType> tableTypes, final List<BgpParameters> bgpParameters) {
        if (bgpParameters != null && !bgpParameters.isEmpty()) {
            for (final BgpParameters parameters : bgpParameters) {
                for (final OptionalCapabilities optionalCapabilities : parameters.getOptionalCapabilities()) {
                    final CParameters cParam = optionalCapabilities.getCParameters();
                    final CParameters1 capabilities = cParam.getAugmentation(CParameters1.class);
                    if (capabilities != null) {
                        final MultiprotocolCapability mc = capabilities.getMultiprotocolCapability();
                        if (mc != null) {
                            this.multiProtocolCapability = true;
                        }
                        if (capabilities.getGracefulRestartCapability() != null) {
                            this.gracefulRestartCapability = true;
                        }
                        if (capabilities.getAddPathCapability() != null) {
                            this.addPathCapability = true;
                        }
                        if (capabilities.getRouteRefreshCapability() != null) {
                            this.routerRefreshCapability = true;
                        }
                    }
                    if (cParam.getAs4BytesCapability() != null) {
                        this.asn32Capability = true;
                    }
                }
            }
        }

        this.holdTimerValue = holdTimerValue;
        this.remoteAddress = StrictBGPPeerRegistry.getIpAddress(remoteAddress);
        this.remotePort = new PortNumber(((InetSocketAddress) remoteAddress).getPort());
        this.localPort = new PortNumber(((InetSocketAddress) localAddress).getPort());
    }

    @Override
    public synchronized State getSessionState() {
        return this.sessionState;
    }

    @Override
    public synchronized void setSessionState(@Nonnull State state) {
        if (state == State.IDLE) {
            this.sessionStopwatch.reset();
        } else if (state == State.UP) {
            this.sessionStopwatch.start();
        }
        this.sessionState = state;
    }

    @Override
    public boolean isAddPathCapabilitySupported() {
        return this.addPathCapability;
    }

    @Override
    public boolean isAsn32CapabilitySupported() {
        return this.asn32Capability;
    }

    @Override
    public boolean isGracefulRestartCapabilitySupported() {
        return this.gracefulRestartCapability;
    }

    @Override
    public boolean isMultiProtocolCapabilitySupported() {
        return this.multiProtocolCapability;
    }

    @Override
    public boolean isRouterRefreshCapabilitySupported() {
        return this.routerRefreshCapability;
    }

    @Override
    public PortNumber getLocalPort() {
        return this.localPort;
    }

    @Override
    public IpAddress getRemoteAddress() {
        return this.remoteAddress;
    }

    @Nonnull
    @Override
    public PortNumber getRemotePort() {
        return this.remotePort;
    }

    @Override
    public long getNegotiatedHoldTime() {
        return this.holdTimerValue;
    }

    @Override
    public long getUpTime() {
        return this.sessionStopwatch.elapsed(TimeUnit.MILLISECONDS);
    }

    public void registerMessagesCounter(final BGPMessagesListener bgpMessagesListener) {
        this.messagesListenerCounter= bgpMessagesListener;
    }
}
