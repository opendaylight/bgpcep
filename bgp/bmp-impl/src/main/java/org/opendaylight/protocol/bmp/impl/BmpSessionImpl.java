package org.opendaylight.protocol.bmp.impl;

import io.netty.channel.Channel;

import java.io.IOException;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.api.BmpSessionListener;
import org.opendaylight.protocol.framework.AbstractProtocolSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.TerminationMessage;
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
    protected void handleMessage(final Notification msg) {
        if (this.state == State.INITIATED) {
            this.listener.onMessage(this, msg);
            if (msg instanceof TerminationMessage) {
                this.close();
            }
        } else {
            if (msg instanceof InitiationMessage) {
                this.state = State.INITIATED;
                this.listener.onMessage(this, msg);
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

    @Override
    public void close() {
        LOG.info("Closing session: {}", this);

        if (this.state != State.IDLE) {
            this.channel.close();
            this.state = State.IDLE;
        }
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
