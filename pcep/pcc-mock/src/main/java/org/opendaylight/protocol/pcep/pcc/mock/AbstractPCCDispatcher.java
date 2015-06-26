package org.opendaylight.protocol.pcep.pcc.mock;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 26.6.2015.
 */
public abstract class AbstractPCCDispatcher  implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPCCDispatcher.class);
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final EventExecutor executor;

    protected AbstractPCCDispatcher(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this(GlobalEventExecutor.INSTANCE, bossGroup, workerGroup);
    }

    protected AbstractPCCDispatcher(EventExecutor executor, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this.bossGroup = (EventLoopGroup) Preconditions.checkNotNull(bossGroup);
        this.workerGroup = (EventLoopGroup) Preconditions.checkNotNull(workerGroup);
        this.executor = (EventExecutor) Preconditions.checkNotNull(executor);
    }

    protected ChannelFuture createServer(InetSocketAddress address, ChannelPipelineInitializer initializer) {
        return this.createServer(address, NioServerSocketChannel.class, initializer);
    }

    protected <CH extends Channel> ChannelFuture createServer(SocketAddress address, Class<? extends ServerChannel> channelClass, final AbstractPCCDispatcher.ChannelPipelineInitializer initializer) {
        ServerBootstrap b = new ServerBootstrap();
        b.childHandler(new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel ch) {
                initializer.initializeChannel(ch, new DefaultPromise(AbstractPCCDispatcher.this.executor));
            }
        });
        b.option(ChannelOption.SO_BACKLOG, Integer.valueOf(128));
        if (!LocalServerChannel.class.equals(channelClass)) {
            b.childOption(ChannelOption.SO_KEEPALIVE, Boolean.valueOf(true));
            b.childOption(ChannelOption.TCP_NODELAY, Boolean.valueOf(true));
        }

        b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        this.customizeBootstrap(b);
        if (b.group() == null) {
            b.group(this.bossGroup, this.workerGroup);
        }

        try {
            b.channel(channelClass);
        } catch (IllegalStateException var6) {
            LOG.trace("Not overriding channelFactory on bootstrap {}", b, var6);
        }

        ChannelFuture f = b.bind(address);
        LOG.debug("Initiated server {} at {}.", f, address);
        return f;
    }

    public void customizeBootstrap(ServerBootstrap b) {
    }

    protected Future<PCEPSessionImpl> createClient(InetSocketAddress address, ReconnectStrategy strategy, final ChannelPipelineInitializer initializer) {
        Bootstrap b = new Bootstrap();
        final PCCProtocolSessionPromise p = new PCCProtocolSessionPromise(this.executor, address, strategy, b);
        ((Bootstrap) b.option(ChannelOption.SO_KEEPALIVE, Boolean.valueOf(true))).handler(new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel ch) {
                initializer.initializeChannel(ch, p);
            }
        });
        this.customizeBootstrap(b);
        this.setWorkerGroup(b);
        this.setChannelFactory(b);
        p.connect();
        LOG.debug("Client created.");
        return p;
    }

    private void setWorkerGroup(Bootstrap b) {
        if (b.group() == null) {
            b.group(this.workerGroup);
        }
    }

    protected Future<PCEPSessionImpl> createClient(InetSocketAddress address, ReconnectStrategy strategy, Bootstrap
        bootstrap, final ChannelPipelineInitializer initializer) {
        final PCCProtocolSessionPromise p = new PCCProtocolSessionPromise(this.executor, address, strategy, bootstrap);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel ch) {
                initializer.initializeChannel(ch, p);
            }
        });
        p.connect();
        LOG.debug("Client created.");
        return p;
    }

    protected void customizeBootstrap(Bootstrap b) {
    }

    protected Future<Void> createReconnectingClient(InetSocketAddress address, ReconnectStrategyFactory connectStrategyFactory, ChannelPipelineInitializer initializer) {
        Bootstrap b = new Bootstrap();
        PCCPReconnectPromise p = new PCCPReconnectPromise(GlobalEventExecutor.INSTANCE, this, address, connectStrategyFactory, b, initializer);
        b.option(ChannelOption.SO_KEEPALIVE, Boolean.valueOf(true));
        this.customizeBootstrap(b);
        this.setWorkerGroup(b);
        this.setChannelFactory(b);
        p.connect();
        return p;
    }

    private void setChannelFactory(Bootstrap b) {
        try {
            b.channel(NioSocketChannel.class);
        } catch (IllegalStateException var3) {
            LOG.trace("Not overriding channelFactory on bootstrap {}", b, var3);
        }

    }

    /**
     * @deprecated
     */
    @Deprecated
    public void close() {
        try {
            this.workerGroup.shutdownGracefully();
        } finally {
            this.bossGroup.shutdownGracefully();
        }

    }

    protected interface ChannelPipelineInitializer {
        void initializeChannel(SocketChannel var1, Promise<PCEPSessionImpl> var2);
    }
}

