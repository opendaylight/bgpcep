/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.mock;

import static org.opendaylight.protocol.util.Ipv4Util.incrementIpv4Address;
import static org.opendaylight.protocol.util.Ipv4Util.incrementIpv4Prefix;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.AdjRibInType;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BmpMockSession extends SimpleChannelInboundHandler<Notification> implements BmpSession {

    private static final Logger LOG = LoggerFactory.getLogger(BmpMockSession.class);

    private static final Ipv4Address NEXT_HOP = new Ipv4Address("1.1.1.1");
    private static final Ipv4Prefix PREFIX = new Ipv4Prefix("1.1.1.1/32");
    private static final Ipv4Address PEER_ADDRESS = NEXT_HOP;

    private final int peersCount;
    private final int prePolicyRoutesCount;
    private final int postPolicyRoutesCount;

    private InetSocketAddress remoteAddress;
    private Channel channel;

    public BmpMockSession(final int peersCount, final int prePolicyRoutesCount, final int postPolicyRoutesCount) {
        this.peersCount = peersCount;
        this.prePolicyRoutesCount = prePolicyRoutesCount;
        this.postPolicyRoutesCount = postPolicyRoutesCount;
    }

    @Override
    public void close() throws InterruptedException {
        LOG.info("BMP session {} is closed.", BmpMockSession.this.channel);
        this.channel.close().sync();
    }

    @Override
    public InetAddress getRemoteAddress() {
        return this.remoteAddress.getAddress();
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final Notification msg) throws Exception {
        // nothing to read
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        this.channel = ctx.channel();
        this.channel.closeFuture().addListener((ChannelFutureListener) future -> LOG.info("BMP session {} final successfully established.", BmpMockSession.this.channel));
        LOG.info("BMP session {} successfully established.", this.channel);
        final InetSocketAddress localAddress = (InetSocketAddress) this.channel.localAddress();
        this.remoteAddress = (InetSocketAddress) this.channel.remoteAddress();
        advertizePeers(this.channel, localAddress);
    }

    private void advertizePeers(final Channel channel, final InetSocketAddress localAddress) {
        channel.writeAndFlush(BmpMockUtil.createInitiation());
        Ipv4Address peerAddress = PEER_ADDRESS;
        for (int i = 0; i < this.peersCount; i++) {
            channel.writeAndFlush(BmpMockUtil.createPeerUp(peerAddress, localAddress.getAddress()));
            LOG.debug("BMP router {} advertized peer {}", channel.localAddress(), peerAddress);
            advertizeRoutes(this.prePolicyRoutesCount, AdjRibInType.PrePolicy, channel, peerAddress);
            advertizeRoutes(this.postPolicyRoutesCount, AdjRibInType.PostPolicy, channel, peerAddress);
            peerAddress = incrementIpv4Address(peerAddress);
        }
    }

    private static void advertizeRoutes(final int count, final AdjRibInType type, final Channel channel,
            final Ipv4Address peerAddress) {
        Ipv4Prefix prefix = PREFIX;
        for (int i = 0; i < count; i++) {
            channel.writeAndFlush(BmpMockUtil.createRouteMonitoring(peerAddress, type, prefix));
            prefix = incrementIpv4Prefix(prefix);
        }
    }

}
