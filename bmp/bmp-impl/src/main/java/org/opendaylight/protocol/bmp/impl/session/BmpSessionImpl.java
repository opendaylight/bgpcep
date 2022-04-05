/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl.session;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.api.BmpSessionListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.Reason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.TerminationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.termination.Tlvs;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BmpSessionImpl extends SimpleChannelInboundHandler<Notification<?>> implements BmpSession {

    private static final Logger LOG = LoggerFactory.getLogger(BmpSessionImpl.class);

    private final BmpSessionListener listener;
    @GuardedBy("this")
    private Channel channel;
    @GuardedBy("this")
    private State state;

    public BmpSessionImpl(final @NonNull BmpSessionListener listener) {
        this.listener = requireNonNull(listener);
        state = State.IDLE;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext channelHandlerContext, final Notification<?> msg) {
        this.handleMessage(msg);
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
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
    public synchronized void channelActive(final ChannelHandlerContext ctx) {
        channel = ctx.channel();
        LOG.info("Starting session {} <-> {}.", channel.localAddress(), channel.remoteAddress());
        Preconditions.checkArgument(State.IDLE == state);
        listener.onSessionUp(this);
        state = State.UP;
    }

    @Override
    public synchronized void close() {
        LOG.info("Closing session: {}", this);
        if (channel != null) {
            channel.close();
            channel = null;
            state = State.IDLE;
        }
    }

    @Override
    public synchronized InetAddress getRemoteAddress() {
        final InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
        requireNonNull(address, "BMP Channel doesn't have a valid remote address.");
        return address.getAddress();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        LOG.error("Exception caught in BMP Session.", cause);
        close();
        listener.onSessionDown(new IllegalStateException(cause));
    }

    @Override
    public String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    private synchronized ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        toStringHelper.add("channel", channel);
        return toStringHelper;
    }

    private synchronized void handleMessage(final Notification<?> msg) {
        switch (state) {
            case UP:
                if (msg instanceof InitiationMessage) {
                    state = State.INITIATED;
                    listener.onMessage(msg);
                } else {
                    LOG.warn("Unexpected message received {}, expected was BMP Initiation Message. Closing session.",
                        msg);
                    close();
                }
                break;
            case INITIATED:
                if (msg instanceof TerminationMessage) {
                    LOG.info("Session {} terminated by remote with reason: {}",
                            this, getTerminationReason((TerminationMessage) msg));
                    close();
                } else {
                    listener.onMessage(msg);
                }
                break;
            case IDLE:
                throw new IllegalStateException("Received message " + msg
                        + " while BMP Session " + this + " was not active.");
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
        listener.onSessionDown(new IOException("End of input detected. Closing the session."));
    }

    protected enum State {
        /**
         * Waiting for connection to be established.
         */
        IDLE,
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
