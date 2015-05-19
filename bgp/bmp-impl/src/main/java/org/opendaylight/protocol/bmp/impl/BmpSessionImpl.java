package org.opendaylight.protocol.bmp.impl;

import io.netty.channel.Channel;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.api.BmpSessionListener;
import org.opendaylight.protocol.framework.AbstractProtocolSession;
import org.opendaylight.yangtools.yang.binding.Notification;

public class BmpSessionImpl extends AbstractProtocolSession<Notification> implements BmpSession {

    private final BmpSessionListener listener;

    private final Channel channel;

    @GuardedBy("this")
    private State state = State.UP;

    public enum State {
        UP, INITIATED
    }

    public BmpSessionImpl(final BmpSessionListener listener, final Channel channel) {
        this.listener = listener;
        this.channel = channel;
    }

    @Override
    protected void handleMessage(final Notification msg) {
        // TODO build simple FSM, pass message to listener
    }

    @Override
    protected void endOfInput() {
        // TODO Auto-generated method stub
    }

    @Override
    protected void sessionUp() {
        this.listener.onSessionUp(this);
    }

    @Override
    public void close() {
        //TODO
    }

}
