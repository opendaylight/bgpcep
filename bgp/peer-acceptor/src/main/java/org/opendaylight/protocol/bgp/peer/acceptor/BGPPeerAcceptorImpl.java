/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.peer.acceptor;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerRegistryListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Component(service = { }, factory = BGPPeerAcceptorImpl.FACTORY_NAME)
public final class BGPPeerAcceptorImpl implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(BGPPeerAcceptorImpl.class);

    static final String FACTORY_NAME = "org.opendaylight.protocol.bgp.peer.acceptor.BGPPeerAcceptorImpl";

    private static final int PRIVILEGED_PORTS = 1024;
    private static final String ADDRESS = ".address";
    private static final String BGP_DISPATCHER = ".bgpDispatcher";

    private final BGPDispatcher bgpDispatcher;
    private final InetSocketAddress address;
    private ChannelFuture futureChannel;
    private Registration listenerRegistration;

    @Inject
    public BGPPeerAcceptorImpl(final BGPDispatcher bgpDispatcher) {
        this(bgpDispatcher, new InetSocketAddress(InetAddresses.forString("0.0.0.0"), 1790));
    }

    @Activate
    public BGPPeerAcceptorImpl(final Map<String, Object> properties) {

    }

    public BGPPeerAcceptorImpl(final BGPDispatcher bgpDispatcher, final InetSocketAddress address) {
        this.bgpDispatcher = requireNonNull(bgpDispatcher);
        this.address = requireNonNull(address);
//        if (!PlatformDependent.isWindows() && !PlatformDependent.maybeSuperUser() && port < PRIVILEGED_PORTS) {
//            throw new SecurityException(
//                "Unable to bind port " + port + " while running as non-root user.");
//        }
    }

    public void start() {
        LOG.debug("Instantiating BGP Peer Acceptor : {}", address);

        futureChannel = bgpDispatcher.createServer(address);
        // Validate future success
        futureChannel.addListener(future -> {
            Preconditions.checkArgument(future.isSuccess(), "Unable to start bgp server on %s",
                address, future.cause());
            final Channel channel = futureChannel.channel();
            if (Epoll.isAvailable()) {
                listenerRegistration = bgpDispatcher.getBGPPeerRegistry().registerPeerRegisterListener(
                    new BGPPeerAcceptorImpl.PeerRegistryListenerImpl(channel.config()));
            }
        });
    }

//    private static InetSocketAddress getAddress(final IpAddressNoZone ipAddress, final PortNumber portNumber) {
//        final InetAddress inetAddr;
//        try {
//            inetAddr = InetAddress.getByName(ipAddress.getIpv4AddressNoZone() != null
//                    ? ipAddress.getIpv4AddressNoZone().getValue() : ipAddress.getIpv6AddressNoZone().getValue());
//        } catch (final UnknownHostException e) {
//            throw new IllegalArgumentException("Illegal binding address " + ipAddress, e);
//        }
//        return new InetSocketAddress(inetAddr, portNumber.getValue().toJava());
//    }

    /**
     * This closes the acceptor and no new bgp connections will be accepted. Connections already established will be
     * preserved.
     */
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
                keys.put(IetfInetUtil.inetAddressForNoZone(ip), password);
                channelConfig.setOption(EpollChannelOption.TCP_MD5SIG, keys);
            });
        }

        @Override
        public void onPeerRemoved(final IpAddressNoZone ip) {
            if (keys.remove(IetfInetUtil.inetAddressForNoZone(ip)) != null) {
                channelConfig.setOption(EpollChannelOption.TCP_MD5SIG, keys);
            }
        }
    }
}
