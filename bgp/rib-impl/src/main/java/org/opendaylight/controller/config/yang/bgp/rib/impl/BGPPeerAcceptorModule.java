/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
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
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerRegistryListener;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;

/**
 * BGP peer acceptor that handles incoming bgp connections.
 */
public class BGPPeerAcceptorModule extends org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractBGPPeerAcceptorModule {

    private static final int PRIVILEGED_PORTS = 1024;

    public BGPPeerAcceptorModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BGPPeerAcceptorModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerAcceptorModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // check if unix root user
        if (!PlatformDependent.isWindows() && !PlatformDependent.maybeSuperUser() && getBindingPort().getValue() < PRIVILEGED_PORTS) {
            throw new AccessControlException("Unable to bind port " + getBindingPort().getValue() + " while running as non-root user.");
        }
        // Try to parse address
        try {
            getAddress();
        } catch (final IllegalArgumentException e) {
            throw new JmxAttributeValidationException("Unable to resolve configured address", e, Lists.newArrayList(bindingAddressJmxAttribute, bindingPortJmxAttribute));
        }
    }

    private AutoCloseable listenerRegistration;

    @Override
    public java.lang.AutoCloseable createInstance() {
        final BGPPeerRegistry peerRegistry = getAcceptingPeerRegistryDependency();
        final ChannelFuture futureChannel = getAcceptingBgpDispatcherDependency().createServer(peerRegistry, getAddress());

        // Validate future success
        futureChannel.addListener(future -> {
            Preconditions.checkArgument(future.isSuccess(), "Unable to start bgp server on %s", getAddress(), future.cause());
            final Channel channel = futureChannel.channel();
            if (Epoll.isAvailable()) {
                BGPPeerAcceptorModule.this.listenerRegistration = peerRegistry.registerPeerRegisterListener(new PeerRegistryListenerImpl(channel.config()));
            }
        });

        return () -> {
            // This closes the acceptor and no new bgp connections will be accepted
            // Connections already established will be preserved
            futureChannel.cancel(true);
            futureChannel.channel().close();
            if (BGPPeerAcceptorModule.this.listenerRegistration != null) {
                BGPPeerAcceptorModule.this.listenerRegistration.close();
            }
        };
    }

    private InetSocketAddress getAddress() {
        final InetAddress inetAddr;
        try {
            inetAddr = InetAddress.getByName(getBindingAddress()
                    .getIpv4Address() != null ? getBindingAddress()
                            .getIpv4Address().getValue() : getBindingAddress()
                            .getIpv6Address().getValue());
        } catch (final UnknownHostException e) {
            throw new IllegalArgumentException("Illegal binding address " + getBindingAddress(), e);
        }
        return new InetSocketAddress(inetAddr, getBindingPort().getValue());
    }

    private static final class PeerRegistryListenerImpl implements PeerRegistryListener {

        private final ChannelConfig channelConfig;

        private final KeyMapping keys;

        PeerRegistryListenerImpl(final ChannelConfig channelConfig) {
            this.channelConfig = channelConfig;
            this.keys = KeyMapping.getKeyMapping();
        }

        @Override
        public void onPeerAdded(final IpAddress ip, final BGPSessionPreferences prefs) {
            if (prefs.getMd5Password().isPresent()) {
                this.keys.put(IetfInetUtil.INSTANCE.inetAddressFor(ip), prefs.getMd5Password().get());
                this.channelConfig.setOption(EpollChannelOption.TCP_MD5SIG, this.keys);
            }
        }

        @Override
        public void onPeerRemoved(final IpAddress ip) {
            if (this.keys.remove(IetfInetUtil.INSTANCE.inetAddressFor(ip)) != null) {
                this.channelConfig.setOption(EpollChannelOption.TCP_MD5SIG, this.keys);
            }
        }

    }

}
