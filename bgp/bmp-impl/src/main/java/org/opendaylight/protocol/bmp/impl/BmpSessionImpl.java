package org.opendaylight.protocol.bmp.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Future;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.api.BmpSessionListener;
import org.opendaylight.protocol.framework.AbstractProtocolSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Termination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.TerminationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.TerminationMessageBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BmpSessionImpl extends AbstractProtocolSession<Notification> implements BmpSession {

    private static final Logger LOG = LoggerFactory.getLogger(BmpSessionImpl.class);

    private final BmpSessionListener listener;

    private final Channel channel;

    @GuardedBy("this")
    private State state = State.UP;

    public BmpSessionImpl(final BmpSessionListener listener, final Channel channel) {
        this.listener = listener;
        this.channel = channel;
    }

    @Override
    protected synchronized void handleMessage(final Notification msg) {
        if (this.state == State.INITIATED) {
            this.listener.onMessage(this, msg);
            if (msg instanceof TerminationMessage) {
                this.close();
            }
        } else if (this.state == State.IDLE) {
            LOG.info("Session has been closed: {}", msg);
        } else {
            if (msg instanceof InitiationMessage) {
                this.state = State.INITIATED;
                this.listener.onMessage(this, msg);
            } else {
                LOG.trace("Closing session, initialization message was expected", msg);
                close();
            }
        }
    }

    @Override
    protected void endOfInput() {
        if (this.state == State.UP) {
            this.listener.onSessionDown(this, new IOException("End of input detected. Close the session."));
        }
    }

    @Override
    protected void sessionUp() {
        this.listener.onSessionUp(this);
    }

    public synchronized State getState() {
        return this.state;
    }

    @Override
    public synchronized void close() {
        LOG.info("Closing session: {}", this);
        // TODO Check whether it should be sned or not termination message to the Router
        this.sendMessage(new TerminationMessageBuilder().setReason(Termination.Reason.forValue(1)).build());

        if (this.state != State.IDLE) {
            this.channel.close();
            this.state = State.IDLE;
        }
    }

    @Override
    public Future<Void> sendMessage(final Notification message) {
        final ChannelFuture f = this.channel.writeAndFlush(message);

        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture arg) {
                if (arg.isSuccess()) {
                    LOG.trace("Message sent to socket: {}", message);
                } else {
                    LOG.debug("Message not sent: {}", message, arg.cause());
                }
            }
        });

        return f;
    }

    @Override
    public InetAddress getRemoteAddress() {
        return ((InetSocketAddress) this.channel.remoteAddress()).getAddress();
    }

    public enum State {
        /**
         * The session has been completely established.
         */
        UP,
        /**
         * The session is half-alive
         */
        INITIATED,
        /**
         * The session has been closed. It will not be resurrected.
         */
        IDLE
    }

}
