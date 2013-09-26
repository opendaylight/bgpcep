package org.opendaylight.protocol.framework;

import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class SimpleDispatcher<M, S extends ProtocolSession<?>, L extends SessionListener<?, ?, ?>> extends
AbstractDispatcher<S, L> {

	private static final Logger logger = LoggerFactory.getLogger(SimpleDispatcher.class);

	private final SessionNegotiatorFactory<M, S, L> negotiatorFactory;
	private final ProtocolHandlerFactory<?> factory;

	public SimpleDispatcher(final SessionNegotiatorFactory<M, S, L> negotiatorFactory, final ProtocolHandlerFactory<?> factory,
			final Promise<S> promise) {
		this.negotiatorFactory = Preconditions.checkNotNull(negotiatorFactory);
		this.factory = Preconditions.checkNotNull(factory);
	}

	@Override
	public void initializeChannel(final SocketChannel ch, final Promise<S> promise, final SessionListenerFactory<L> lfactory) {
		ch.pipeline().addLast(this.factory.getDecoders());
		ch.pipeline().addLast("negotiator", this.negotiatorFactory.getSessionNegotiator(lfactory, ch, promise));
		ch.pipeline().addLast(this.factory.getEncoders());
		logger.debug("initialization completed for channel {}", ch);
	}
}
