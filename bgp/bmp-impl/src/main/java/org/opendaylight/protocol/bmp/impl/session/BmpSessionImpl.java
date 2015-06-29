/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.session;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.api.BmpSessionListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Reason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.TerminationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.termination.Tlvs;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BmpSessionImpl extends SimpleChannelInboundHandler<Notification> implements BmpSession {

    private static final Logger LOG = LoggerFactory.getLogger(BmpSessionImpl.class);

    private final BmpSessionListener listener;

    private final Channel channel;

    private State state = State.UP;

    public BmpSessionImpl(@Nonnull final BmpSessionListener listener, @Nonnull final Channel channel) {
        this.listener = Preconditions.checkNotNull(listener);
        this.channel = Preconditions.checkNotNull(channel);
        LOG.info("Session {} <-> {} started.", channel.localAddress(), channel.remoteAddress());
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext channelHandlerContext, final Notification msg) throws Exception {
        this.handleMessage(msg);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        LOG.debug("Channel {} inactive.", ctx.channel());
        this.endOfInput();

        try {
            super.channelInactive(ctx);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to delegate channel inactive event on channel " + ctx.channel(), e);
        }
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {
        this.sessionUp();
    }

    @Override
    public synchronized void close() {
        LOG.info("Closing session: {}", this);
        this.channel.close();
    }

    @Override
    public InetAddress getRemoteAddress() {
        return ((InetSocketAddress) this.channel.remoteAddress()).getAddress();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        LOG.error("Exception caught in BMP Session.", cause);
        close();
    }

    @Override
    public String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        toStringHelper.add("channel", this.channel);
        return toStringHelper;
    }

    private synchronized void handleMessage(final Notification msg) {
        switch (this.state) {
        case UP:
            if (msg instanceof InitiationMessage) {
                this.state = State.INITIATED;
                this.listener.onMessage(this, msg);
            } else {
                close();
            }
            break;
        case INITIATED:
            if (msg instanceof TerminationMessage) {
                LOG.info("Session {} terminated by remote with reason: {}", this, getTerminationReason((TerminationMessage) msg));
                close();
            } else {
                this.listener.onMessage(this, msg);
            }
            break;
        default:
            break;
        }
    }

    private static Reason getTerminationReason(final TerminationMessage terminationMessage) {
        final Tlvs tlvs = terminationMessage.getTlvs();
        if (tlvs != null && tlvs.getReasonTlv() != null) {
            return tlvs.getReasonTlv().getReason();
        }
        return null;
    }

    private void endOfInput() {
        this.listener.onSessionDown(this, new IOException("End of input detected. Closing the session."));
    }

    private void sessionUp() {
        this.listener.onSessionUp(this);
    }

    protected enum State {
        /**
         * The connection has been established. Waiting for Initiation Message.
         */
        UP,
        /**
         * The Initiation Messages has been received. Pass incoming messages to session listener.
         */
        INITIATED
    }

}
