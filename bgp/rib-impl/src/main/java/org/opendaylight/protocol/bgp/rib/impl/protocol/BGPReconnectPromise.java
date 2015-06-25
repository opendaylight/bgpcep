package org.opendaylight.protocol.bgp.rib.impl.protocol;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.bgp.rib.impl.BGPDispatcherImpl;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionImpl;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 25.6.2015.
 */
public class BGPReconnectPromise extends DefaultPromise<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(BGPReconnectPromise.class);

    private final BGPDispatcherImpl dispatcher;
    private final InetSocketAddress address;
    private final ReconnectStrategyFactory strategyFactory;
    private final Bootstrap b;
    private final BGPDispatcherImpl.ChannelPipelineInitializer initializer;
    private Future<BGPSessionImpl> pending;
    private final EventExecutor executor;

    public BGPReconnectPromise(final EventExecutor executor, final BGPDispatcherImpl dispatcher, final InetSocketAddress address,
                               final ReconnectStrategyFactory connectStrategyFactory, final Bootstrap b, final BGPDispatcherImpl.ChannelPipelineInitializer initializer) {
        super(executor);
        this.executor = executor;
        this.b = b;
        this.initializer = Preconditions.checkNotNull(initializer);
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
        this.address = Preconditions.checkNotNull(address);
        this.strategyFactory = Preconditions.checkNotNull(connectStrategyFactory);
    }

    public synchronized void connect() {
        final ReconnectStrategy cs = this.strategyFactory.createReconnectStrategy();

        // Set up a client with pre-configured bootstrap, but add a closed channel handler into the pipeline to support reconnect attempts
        pending = createClient(this.address, cs, b, new BGPDispatcherImpl.ChannelPipelineInitializer() {
            @Override
            public void initializeChannel(final SocketChannel channel, final Promise<BGPSessionImpl> promise) {
                initializer.initializeChannel(channel, promise);
                // add closed channel handler
                // This handler has to be added as last channel handler and the channel inactive event has to be caught by it
                // Handlers in front of it can react to channelInactive event, but have to forward the event or the reconnect will not work
                // This handler is last so all handlers in front of it can handle channel inactive (to e.g. resource cleanup) before a new connection is started
                channel.pipeline().addLast(new ClosedChannelHandler(BGPReconnectPromise.this));
            }
        });

        pending.addListener(new GenericFutureListener<Future<Object>>() {
            @Override
            public void operationComplete(Future<Object> future) throws Exception {
                if (!future.isSuccess()) {
                    BGPReconnectPromise.this.setFailure(future.cause());
                }
            }
        });
    }

    public Future<BGPSessionImpl> createClient(InetSocketAddress address, ReconnectStrategy strategy, Bootstrap bootstrap,
                                               final BGPDispatcherImpl.ChannelPipelineInitializer initializer) {
        final BGPProtocolSessionPromise p = new BGPProtocolSessionPromise(this.executor, address, strategy, bootstrap);
        ChannelHandler chInit = BGPDispatcherImpl.BGPChannel.createChannelInitializer(initializer, p);
        bootstrap.handler(chInit);
        p.connect();
        LOG.debug("Client created.");
        return p;
    }

    /**
     * @return true if initial connection was established successfully, false if initial connection failed due to e.g. Connection refused, Negotiation failed
     */
    private boolean isInitialConnectFinished() {
        Preconditions.checkNotNull(pending);
        return pending.isDone() && pending.isSuccess();
    }

    @Override
    public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
        if (super.cancel(mayInterruptIfRunning)) {
            Preconditions.checkNotNull(pending);
            this.pending.cancel(mayInterruptIfRunning);
            return true;
        }

        return false;
    }

    /**
     * Channel handler that responds to channelInactive event and reconnects the session.
     * Only if the promise was not canceled.
     */
    private static final class ClosedChannelHandler extends ChannelInboundHandlerAdapter {
        private final BGPReconnectPromise promise;

        public ClosedChannelHandler(final BGPReconnectPromise promise) {
            this.promise = promise;
        }

        @Override
        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            // This is the ultimate channel inactive handler, not forwarding
            if (promise.isCancelled()) {
                return;
            }

            if (!promise.isInitialConnectFinished()) {
                LOG.debug("Connection to {} was dropped during negotiation, reattempting", promise.address);
            }

            LOG.debug("Reconnecting after connection to {} was dropped", promise.address);
            promise.connect();
        }
    }
}
