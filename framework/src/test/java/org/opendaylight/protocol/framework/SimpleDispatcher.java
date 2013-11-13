package org.opendaylight.protocol.framework;

import com.google.common.base.Preconditions;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class SimpleDispatcher extends AbstractDispatcher<SimpleSession, SimpleSessionListener> {

	private static final Logger logger = LoggerFactory.getLogger(SimpleDispatcher.class);

	private final SessionNegotiatorFactory<SimpleMessage, SimpleSession, SimpleSessionListener> negotiatorFactory;
	private final ProtocolHandlerFactory<?> factory;


	private final class SimplePipelineInitializer implements PipelineInitializer<SimpleSession> {
		final SessionListenerFactory<SimpleSessionListener> listenerFactory;

		SimplePipelineInitializer(final SessionListenerFactory<SimpleSessionListener> listenerFactory) {
			this.listenerFactory = Preconditions.checkNotNull(listenerFactory);
		}

		@Override
		public void initializeChannel(final SocketChannel channel, final Promise<SimpleSession> promise) {
			channel.pipeline().addLast(factory.getDecoders());
			channel.pipeline().addLast("negotiator", negotiatorFactory.getSessionNegotiator(listenerFactory, channel, promise));
			channel.pipeline().addLast(factory.getEncoders());
			logger.debug("initialization completed for channel {}", channel);
		}

	}

	public SimpleDispatcher(final SessionNegotiatorFactory<SimpleMessage, SimpleSession, SimpleSessionListener> negotiatorFactory, final ProtocolHandlerFactory<?> factory,
			final Promise<SimpleSession> promise, EventLoopGroup eventLoopGroup) {
		super(eventLoopGroup, eventLoopGroup);
		this.negotiatorFactory = Preconditions.checkNotNull(negotiatorFactory);
		this.factory = Preconditions.checkNotNull(factory);
	}

	public Future<SimpleSession> createClient(final InetSocketAddress address, final ReconnectStrategy strategy, final SessionListenerFactory<SimpleSessionListener> listenerFactory) {
		return super.createClient(address, strategy, new SimplePipelineInitializer(listenerFactory));
	}

	public ChannelFuture createServer(final InetSocketAddress address, final SessionListenerFactory<SimpleSessionListener> listenerFactory) {
		return super.createServer(address, new SimplePipelineInitializer(listenerFactory));
	}

    @Override
    public void close() {
    }
}
