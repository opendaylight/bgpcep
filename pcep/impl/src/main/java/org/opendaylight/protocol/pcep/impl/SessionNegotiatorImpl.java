package org.opendaylight.protocol.pcep.impl;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;
import java.util.concurrent.ExecutionException;
import org.opendaylight.protocol.pcep.SessionNegotiator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 29.6.2015.
 */
public abstract class SessionNegotiatorImpl extends ChannelInboundHandlerAdapter implements SessionNegotiator {
    private static final Logger LOG = LoggerFactory.getLogger(SessionNegotiatorImpl.class);
    protected final Channel channel;
    protected final Promise<PCEPSessionImpl> promise;

    protected SessionNegotiatorImpl(final Channel channel, final Promise<PCEPSessionImpl> promise) {
        this.promise = Preconditions.checkNotNull(promise);
        this.channel = Preconditions.checkNotNull(channel);
    }

    protected final void negotiationSuccessful(PCEPSessionImpl session) {
        this.LOG.debug("Negotiation on channel {} successful with session {}", this.channel, session);
        this.channel.pipeline().replace(this, "session", session);
        this.promise.setSuccess(session);
    }

    protected void negotiationFailed(Throwable cause) {
        this.LOG.debug("Negotiation on channel {} failed", this.channel, cause);
        this.channel.close();
        this.promise.setFailure(cause);
    }

    protected final void sendMessage(final Message msg) {
        this.channel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture f) {
                if (!f.isSuccess()) {
                    LOG.info("Failed to send message {}", msg, f.cause());
                    negotiationFailed(f.cause());
                } else {
                    LOG.trace("Message {} sent to socket", msg);
                }

            }
        });
    }

    @Override
    public final void channelActive(ChannelHandlerContext ctx) {
        this.LOG.debug("Starting session negotiation on channel {}", this.channel);

        try {
            this.startNegotiation();
        } catch (final Exception e) {
            this.LOG.warn("Unexpected negotiation failure", e);
            this.negotiationFailed(e);
        }

    }

    @Override
    public final void channelRead(ChannelHandlerContext ctx, Object msg) {
        this.LOG.debug("Negotiation read invoked on channel {}", this.channel);

        try {
            this.handleMessage((Message) msg);
        } catch (Exception var4) {
            this.LOG.debug("Unexpected error while handling negotiation message {}", msg, var4);
            this.negotiationFailed(var4);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        this.LOG.info("Unexpected error during negotiation", cause);
        this.negotiationFailed(cause);
    }

    protected abstract void startNegotiation() throws ExecutionException;

    protected abstract void handleMessage(final Message msg);
}
