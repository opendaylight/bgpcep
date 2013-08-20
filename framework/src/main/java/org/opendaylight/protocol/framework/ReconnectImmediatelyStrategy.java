package org.opendaylight.protocol.framework;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.Preconditions;

/**
 * Utility ReconnectStrategy singleton, which will cause the reconnect process
 * to immediately schedule a reconnection attempt.
 */
@ThreadSafe
public final class ReconnectImmediatelyStrategy implements ReconnectStrategy {
	private final EventExecutor executor;
	private final int timeout;

	public ReconnectImmediatelyStrategy(final EventExecutor executor, final int timeout) {
		Preconditions.checkArgument(timeout >= 0);
		this.executor = Preconditions.checkNotNull(executor);
		this.timeout = timeout;
	}

	@Override
	public Future<Void> scheduleReconnect() {
		return executor.newSucceededFuture(null);
	}

	@Override
	public void reconnectSuccessful() {
		// Nothing to do
	}

	@Override
	public int getConnectTimeout() {
		return timeout;
	}
}
