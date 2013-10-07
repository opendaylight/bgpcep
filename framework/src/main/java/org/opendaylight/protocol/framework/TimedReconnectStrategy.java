package org.opendaylight.protocol.framework;

import com.google.common.base.Preconditions;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Swiss army knife equivalent for reconnect strategies.
 * 
 * This strategy continues to schedule reconnect attempts, each having to
 * complete in a fixed time (connectTime).
 * 
 * Initial sleep time is specified as minSleep. Each subsequent unsuccessful
 * attempt multiplies this time by a constant factor (sleepFactor) -- this
 * allows for either constant reconnect times (sleepFactor = 1), or various
 * degrees of exponential back-off (sleepFactor > 1). Maximum sleep time
 * between attempts can be capped to a specific value (maxSleep).
 * 
 * The strategy can optionally give up based on two criteria:
 * 
 * A preset number of connection retries (maxAttempts) has been reached, or
 * 
 * A preset absolute deadline is reached (deadline nanoseconds, as reported
 * by System.nanoTime(). In this specific case, both connectTime and maxSleep
 * will be controlled such that the connection attempt is resolved as closely
 * to the deadline as possible.
 * 
 * Both these caps can be combined, with the strategy giving up as soon as the
 * first one is reached.
 */
@ThreadSafe
public final class TimedReconnectStrategy implements ReconnectStrategy {
	private static final Logger logger = LoggerFactory.getLogger(TimedReconnectStrategy.class);
	private final EventExecutor executor;
	private final Long deadline, maxAttempts, maxSleep;
	private final double sleepFactor;
	private final int connectTime;
	private final long minSleep;

	@GuardedBy("this")
	private long attempts;

	@GuardedBy("this")
	private long lastSleep;

	@GuardedBy("this")
	private boolean scheduled;

	public TimedReconnectStrategy(final EventExecutor executor, final int connectTime,
			final long minSleep, final double sleepFactor, final Long maxSleep,
			final Long maxAttempts, final Long deadline) {
		Preconditions.checkArgument(maxSleep == null || minSleep <= maxSleep);
		Preconditions.checkArgument(sleepFactor >= 1);
		Preconditions.checkArgument(connectTime >= 0);
		this.executor = Preconditions.checkNotNull(executor);
		this.deadline = deadline;
		this.maxAttempts = maxAttempts;
		this.minSleep = minSleep;
		this.maxSleep = maxSleep;
		this.sleepFactor = sleepFactor;
		this.connectTime = connectTime;
	}

	@Override
	public synchronized Future<Void> scheduleReconnect(final Throwable cause) {
		logger.debug("Connection attempt failed", cause);

		// Check if a reconnect attempt is scheduled
		Preconditions.checkState(scheduled == false);

		// Get a stable 'now' time for deadline calculations
		final long now = System.nanoTime();

		// Obvious stop conditions
		if (maxAttempts != null && attempts >= maxAttempts) {
			return executor.newFailedFuture(new Throwable("Maximum reconnection attempts reached"));
		}
		if (deadline != null && deadline <= now) {
			return executor.newFailedFuture(new TimeoutException("Reconnect deadline reached"));
		}

		/*
		 * First connection attempt gets initialized to minimum sleep,
		 * each subsequent is exponentially backed off by sleepFactor.
		 */
		if (attempts != 0) {
			lastSleep *= sleepFactor;
		} else {
			lastSleep = minSleep;
		}

		// Cap the sleep time to maxSleep
		if (maxSleep != null && lastSleep > maxSleep) {
			lastSleep = maxSleep;
		}

		// Check if the reconnect attempt is within the deadline
		if (deadline != null && deadline <= now + TimeUnit.MILLISECONDS.toNanos(lastSleep)) {
			return executor.newFailedFuture(new TimeoutException("Next reconnect would happen after deadline"));
		}

		// If we are not sleeping at all, return an already-succeeded future
		if (lastSleep == 0) {
			return executor.newSucceededFuture(null);
		}

		// Need to retain a final reference to this for locking purposes,
		// also set the scheduled flag.
		final Object lock = this;
		scheduled = true;
		attempts++;

		// Schedule a task for the right time. It will also clear the flag.
		return executor.schedule(new Callable<Void>() {
			@Override
			public Void call() throws TimeoutException {
				synchronized (lock) {
					Preconditions.checkState(scheduled == true);
					scheduled = false;
				}

				return null;
			}
		}, lastSleep, TimeUnit.MILLISECONDS);
	}

	@Override
	public synchronized void reconnectSuccessful() {
		Preconditions.checkState(scheduled == false);
		attempts = 0;
	}

	@Override
	public int getConnectTimeout() throws TimeoutException {
		int timeout = connectTime;

		if (deadline != null) {

			// If there is a deadline, we may need to cap the connect
			// timeout to meet the deadline.
			final long now = System.nanoTime();
			if (now >= deadline) {
				throw new TimeoutException("Reconnect deadline already passed");
			}

			final long left = TimeUnit.NANOSECONDS.toMillis(deadline - now);
			if (left < 1) {
				throw new TimeoutException("Connect timeout too close to deadline");
			}

			/*
			 * A bit of magic:
			 * - if time left is less than the timeout, set it directly
			 * - if there is no timeout, and time left is:
			 *      - less than maximum integer, set timeout to time left
			 *      - more than maximum integer, set timeout Integer.MAX_VALUE
			 */
			if (timeout > left) {
				timeout = (int) left;
			} else if (timeout == 0) {
				timeout = left <= Integer.MAX_VALUE ? (int) left : Integer.MAX_VALUE;
			}
		}
		return timeout;
	}
}
