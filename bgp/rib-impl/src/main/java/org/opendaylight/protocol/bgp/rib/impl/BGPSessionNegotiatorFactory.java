package org.opendaylight.protocol.bgp.rib.impl;

import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;

import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiator;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;

import com.google.common.base.Preconditions;

public final class BGPSessionNegotiatorFactory implements SessionNegotiatorFactory<BGPMessage, BGPSessionImpl, BGPSessionListener> {
	private final BGPSessionPreferences initialPrefs;
	private final Timer timer;

	public BGPSessionNegotiatorFactory(final Timer timer, final BGPSessionPreferences initialPrefs) {
		this.timer = Preconditions.checkNotNull(timer);
		this.initialPrefs = Preconditions.checkNotNull(initialPrefs);
	}

	@Override
	public SessionNegotiator<BGPSessionImpl> getSessionNegotiator(final SessionListenerFactory<BGPSessionListener> factory,
			final Channel channel, final Promise<BGPSessionImpl> promise) {
		return new BGPSessionNegotiator(timer, promise, channel, initialPrefs, factory.getSessionListener());
	}
}
