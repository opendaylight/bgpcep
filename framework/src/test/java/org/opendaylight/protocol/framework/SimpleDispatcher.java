package org.opendaylight.protocol.framework;

import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class SimpleDispatcher<M extends ProtocolMessage, S extends ProtocolSession<M>, L extends SessionListener<M, ?, ?>> extends
AbstractDispatcher<S> {

	private static final Logger logger = LoggerFactory.getLogger(SimpleDispatcher.class);

	private final SessionNegotiatorFactory<M, S, L> negotiatorFactory;
	private final SessionListenerFactory<L> listenerFactory;
	private final ProtocolHandlerFactory<?> factory;

	public SimpleDispatcher(final SessionNegotiatorFactory<M, S, L> negotiatorFactory, final SessionListenerFactory<L> listenerFactory,
			final ProtocolHandlerFactory<?> factory, final Promise<S> promise) {
		this.negotiatorFactory = Preconditions.checkNotNull(negotiatorFactory);
		this.listenerFactory = Preconditions.checkNotNull(listenerFactory);
		this.factory = Preconditions.checkNotNull(factory);
	}

	@Override
	public void initializeChannel(final SocketChannel ch, final Promise<S> promise) {
		logger.debug("initializing channel {}", ch);
		logger.debug("negotiatorfactory {}", this.negotiatorFactory);
		logger.debug("listenerfactory {}", this.listenerFactory);
		logger.debug("handlerFactory {}", this.factory);
		ch.pipeline().addLast(this.factory.getDecoders());
		logger.debug("decoders set");
		ch.pipeline().addLast("negotiator", this.negotiatorFactory.getSessionNegotiator(this.listenerFactory, ch, promise));
		logger.debug("negotiatior set");
		ch.pipeline().addLast(this.factory.getEncoders());
		logger.debug("encoders set");
		logger.debug("initialization completed for channel {}", ch);
	}
}
