/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.peer.acceptor;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.util.internal.PlatformDependent;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.AccessControlException;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerRegistryListener;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.peer.acceptor.config.rev161003.BgpPeerAcceptorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPPeerAcceptorImpl implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BGPPeerAcceptorImpl.class);
    private static final int PRIVILEGED_PORTS = 1024;
    private final ChannelFuture futureChannel;
    private AutoCloseable listenerRegistration;

    public BGPPeerAcceptorImpl(final BgpPeerAcceptorConfig config, final BGPPeerRegistry peerRegistry, final BGPDispatcher bgpDispatcher) {
        final PortNumber portNumber = config.getBindingPort();
        final IpAddress bindingAddress = config.getBindingAddress();
        LOG.debug("Instantiating BGP Peer Acceptor : {}/{}", bindingAddress, portNumber);

        if (!PlatformDependent.isWindows() && !PlatformDependent.isRoot() && portNumber.getValue() < PRIVILEGED_PORTS) {
            throw new AccessControlException("Unable to bind port " + portNumber.getValue() + " while running as non-root user.");
        }

        this.futureChannel = bgpDispatcher.createServer(peerRegistry, getAddress(bindingAddress, portNumber));

        // Validate future success
        this.futureChannel.addListener(future -> {
            Preconditions.checkArgument(future.isSuccess(), "Unable to start bgp server on %s", getAddress(bindingAddress, portNumber), future.cause());
            final Channel channel = this.futureChannel.channel();
            if (Epoll.isAvailable()) {
                BGPPeerAcceptorImpl.this.listenerRegistration = peerRegistry.registerPeerRegisterListener(new BGPPeerAcceptorImpl.PeerRegistryListenerImpl(channel.config()));
            }
        });
    }

    private static final class PeerRegistryListenerImpl implements PeerRegistryListener {
        private final ChannelConfig channelConfig;
        private final KeyMapping keys;

        PeerRegistryListenerImpl(final ChannelConfig channelConfig) {
            this.channelConfig = channelConfig;
            this.keys = KeyMapping.getKeyMapping();
        }

        @Override
        public void onPeerAdded(@Nonnull final IpAddress ip, @Nonnull final BGPSessionPreferences prefs) {
            if (prefs.getMd5Password().isPresent()) {
                this.keys.put(IetfInetUtil.INSTANCE.inetAddressFor(ip), prefs.getMd5Password().get());
                this.channelConfig.setOption(EpollChannelOption.TCP_MD5SIG, this.keys);
            }
        }

        @Override
        public void onPeerRemoved(@Nonnull final IpAddress ip) {
            if (this.keys.remove(IetfInetUtil.INSTANCE.inetAddressFor(ip)) != null) {
                this.channelConfig.setOption(EpollChannelOption.TCP_MD5SIG, this.keys);
            }
        }
    }

    private InetSocketAddress getAddress(final IpAddress ipAddress, final PortNumber portNumber) {
        final InetAddress inetAddr;
        try {
            inetAddr = InetAddress.getByName(ipAddress.getIpv4Address() != null ? ipAddress.getIpv4Address().getValue() : ipAddress.getIpv6Address().getValue());
        } catch (final UnknownHostException e) {
            throw new IllegalArgumentException("Illegal binding address " + ipAddress, e);
        }
        return new InetSocketAddress(inetAddr, portNumber.getValue());
    }

    /**
     * This closes the acceptor and no new bgp connections will be accepted
     * Connections already established will be preserved
     *
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        this.futureChannel.cancel(true);
        this.futureChannel.channel().close();
        if (BGPPeerAcceptorImpl.this.listenerRegistration != null) {
            BGPPeerAcceptorImpl.this.listenerRegistration.close();
        }
    }
}
