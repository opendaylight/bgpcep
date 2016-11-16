/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.state;

import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.StrictBGPPeerRegistry;
import org.opendaylight.protocol.bgp.state.impl.neighbor.BGPNeighborStateImpl;
import org.opendaylight.protocol.bgp.state.spi.BGPSessionStateListener;
import org.opendaylight.protocol.bgp.state.spi.state.BGPNeighborState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpNeighborState.SessionState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.MultiprotocolCapability;
import org.opendaylight.yangtools.yang.binding.Notification;

public abstract class BGPPeerState implements BGPSessionStateListener {
    protected final BGPNeighborState neighborState;
    protected final BGPTableTypeRegistryConsumer tableTypeRegistry;

    protected BGPPeerState(final IpAddress neighborAddress, final Set<Class<? extends AfiSafiType>> afiSafisAdvertized,
        final BGPTableTypeRegistryConsumer tableTypeRegistry) {
        this.neighborState = new BGPNeighborStateImpl(neighborAddress, afiSafisAdvertized, Collections.emptySet());
        this.tableTypeRegistry = tableTypeRegistry;
    }

    @Override
    public void messageSent(final Notification msg) {
        if (msg instanceof Notify) {
            this.neighborState.increaseNotificationSent();
        } else if (msg instanceof Update) {
            this.neighborState.increaseUpdateSent();
        }
    }

    @Override
    public void messageReceived(final Notification msg) {
        if (msg instanceof Notify) {
            this.neighborState.increaseNotificationReceived();
        } else if (msg instanceof Update) {
            this.neighborState.increaseUpdateReceived();
        }
    }

    @Override
    public void advertizeCapabilities(final int holdTimerValue, final Channel channel, final Set<BgpTableType> tableTypes,
        final List<BgpParameters> bgpParameters) {
        boolean addPath = false;
        boolean asn32 = false;
        boolean gracefulRestart = false;
        boolean multiProtocol = false;
        boolean routerRefresh = false;
        if (bgpParameters != null && !bgpParameters.isEmpty()) {
            for (final BgpParameters parameters : bgpParameters) {
                for (final OptionalCapabilities optionalCapabilities : parameters.getOptionalCapabilities()) {
                    final CParameters cParam = optionalCapabilities.getCParameters();
                    final CParameters1 capabilities = cParam.getAugmentation(CParameters1.class);
                    if (capabilities != null) {
                        final MultiprotocolCapability mc = capabilities.getMultiprotocolCapability();
                        if (mc != null) {
                            multiProtocol = true;
                        }
                        if (capabilities.getGracefulRestartCapability() != null) {
                            gracefulRestart = true;
                        }
                        if (capabilities.getAddPathCapability() != null) {
                            addPath = true;
                        }
                        if (capabilities.getRouteRefreshCapability() != null) {
                            routerRefresh = true;
                        }
                    }
                    if (cParam.getAs4BytesCapability() != null) {
                        asn32 = true;
                    }
                }
            }
        }
        final InetSocketAddress localAddress = ((InetSocketAddress) channel.remoteAddress());
        this.neighborState.setCapabilities(holdTimerValue, localAddress.getPort(), StrictBGPPeerRegistry.getIpAddress(channel.remoteAddress()),
            ((InetSocketAddress) channel.localAddress()).getPort(), addPath, asn32, gracefulRestart, multiProtocol, routerRefresh);
    }

    @Override
    public void setSessionState(final SessionState state) {
        this.neighborState.setState(state);
    }
}
