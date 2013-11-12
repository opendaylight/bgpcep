package org.opendaylight.controller.config.reconnectstrategy.util;

import io.netty.util.concurrent.Future;

import org.opendaylight.protocol.framework.ReconnectStrategy;

final public class ReconnectStrategyCloseable implements ReconnectStrategy,
		AutoCloseable {

	private final ReconnectStrategy inner;

	public ReconnectStrategyCloseable(ReconnectStrategy inner) {
		this.inner = inner;
	}

	@Override
	public void close() throws Exception {

	}

	@Override
	public int getConnectTimeout() throws Exception {
		return this.inner.getConnectTimeout();
	}

	@Override
	public Future<Void> scheduleReconnect(Throwable cause) {
		return this.inner.scheduleReconnect(cause);
	}

	@Override
	public void reconnectSuccessful() {
		this.inner.reconnectSuccessful();
	}
}
