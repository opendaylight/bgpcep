package org.opendaylight.protocol.pcep.impl;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.io.Closeable;
import java.net.SocketAddress;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.tcpmd5.netty.MD5ChannelOption;
import org.opendaylight.tcpmd5.netty.MD5ServerChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 25.6.2015.
 */
public abstract class AbstractPCEPDispatcher implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPCEPDispatcher.class);
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final EventExecutor executor;
    private final MD5ServerChannelFactory<?> scf;
    protected KeyMapping keys;

    protected AbstractPCEPDispatcher(EventLoopGroup bossGroup, EventLoopGroup workerGroup, final MD5ServerChannelFactory<?> scf) {
        this(GlobalEventExecutor.INSTANCE, bossGroup, workerGroup, scf);
    }

    protected AbstractPCEPDispatcher(EventExecutor executor, EventLoopGroup bossGroup, EventLoopGroup workerGroup, final MD5ServerChannelFactory<?> scf) {
        this.bossGroup = Preconditions.checkNotNull(bossGroup);
        this.workerGroup = Preconditions.checkNotNull(workerGroup);
        this.executor = Preconditions.checkNotNull(executor);
        this.scf = scf;
    }

    protected ChannelFuture createServer(SocketAddress address, final AbstractPCEPDispatcher.ChannelPipelineInitializer initializer) {
        final Class<NioServerSocketChannel> channelClass = NioServerSocketChannel.class;
        ServerBootstrap b = new ServerBootstrap();
        b.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                initializer.initializeChannel(ch, new DefaultPromise(AbstractPCEPDispatcher.this.executor));
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

    protected void customizeBootstrap(ServerBootstrap b) {
        if (this.keys != null && !this.keys.isEmpty()) {
            if (this.scf == null) {
                throw new UnsupportedOperationException("No key access instance available, cannot use key mapping");
            }

            LOG.debug("Adding MD5 keys {} to boostrap {}", this.keys, b);
            b.channelFactory(this.scf);
            b.option(MD5ChannelOption.TCP_MD5SIG, this.keys);
        }

        // Make sure we are doing round-robin processing
        b.childOption(ChannelOption.MAX_MESSAGES_PER_READ, 1);
    }

    public void close() {
    }

    protected interface ChannelPipelineInitializer {
        void initializeChannel(SocketChannel var1, Promise<PCEPSessionImpl> var2);
    }
}
