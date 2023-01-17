/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.peer.acceptor;

import static java.util.Objects.requireNonNull;

import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.util.internal.PlatformDependent;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerRegistryListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.peer.acceptor.config.rev200120.BgpPeerAcceptorConfig;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPPeerAcceptorImpl implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BGPPeerAcceptorImpl.class);

    private final InetSocketAddress address;
    private final ChannelFuture futureChannel;
    private final Registration listenerRegistration;

    public BGPPeerAcceptorImpl(final BGPDispatcher bgpDispatcher, final BgpPeerAcceptorConfig config) {
        this(config.requireBindingAddress(), config.requireBindingPort(), bgpDispatcher);
    }

    public BGPPeerAcceptorImpl(final IpAddressNoZone bindingAddress, final PortNumber portNumber,
            final BGPDispatcher bgpDispatcher) {
        address = getAddress(requireNonNull(bindingAddress), requireNonNull(portNumber));
        if (!PlatformDependent.isWindows() && !PlatformDependent.maybeSuperUser()
                && portNumber.getValue().toJava() < 1024) {
            throw new SecurityException("Unable to bind port " + portNumber.getValue()
                    + " while running as non-root user.");
        }

        LOG.debug("Instantiating BGP Peer Acceptor : {}", address);

        futureChannel = bgpDispatcher.createServer(address);
        try {
            futureChannel.sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for channel", e);
        }

        if (Epoll.isAvailable()) {
            listenerRegistration = bgpDispatcher.getBGPPeerRegistry().registerPeerRegisterListener(
                new BGPPeerAcceptorImpl.PeerRegistryListenerImpl(futureChannel.channel().config()));
        } else {
            listenerRegistration = null;
        }
    }

    private static InetSocketAddress getAddress(final IpAddressNoZone ipAddress, final PortNumber portNumber) {
        final InetAddress inetAddr;
        try {
            inetAddr = InetAddress.getByName(ipAddress.getIpv4AddressNoZone() != null
                    ? ipAddress.getIpv4AddressNoZone().getValue() : ipAddress.getIpv6AddressNoZone().getValue());
        } catch (final UnknownHostException e) {
            throw new IllegalArgumentException("Illegal binding address " + ipAddress, e);
        }
        return new InetSocketAddress(inetAddr, portNumber.getValue().toJava());
    }

    /**
     * This closes the acceptor and no new bgp connections will be accepted
     * Connections already established will be preserved.
     **/
    @Override
    public void close() {
        futureChannel.cancel(true);
        futureChannel.channel().close();
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
    }

    private static final class PeerRegistryListenerImpl implements PeerRegistryListener {
        private final Map<InetAddress, byte[]> keys = new HashMap<>();
        private final ChannelConfig channelConfig;

        PeerRegistryListenerImpl(final ChannelConfig channelConfig) {
            this.channelConfig = channelConfig;
        }

        @Override
        public void onPeerAdded(final IpAddressNoZone ip, final BGPSessionPreferences prefs) {
            prefs.getMd5Password().ifPresent(password -> {
                keys.put(IetfInetUtil.INSTANCE.inetAddressForNoZone(ip), password);
                channelConfig.setOption(EpollChannelOption.TCP_MD5SIG, keys);
            });
        }

        @Override
        public void onPeerRemoved(final IpAddressNoZone ip) {
            if (keys.remove(IetfInetUtil.INSTANCE.inetAddressForNoZone(ip)) != null) {
                channelConfig.setOption(EpollChannelOption.TCP_MD5SIG, keys);
            }
        }
    }
}
