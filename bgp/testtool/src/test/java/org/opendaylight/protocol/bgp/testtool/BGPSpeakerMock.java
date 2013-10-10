/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.testtool;

import io.netty.channel.socket.SocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.impl.SimpleBGPMessageFactory;
import org.opendaylight.protocol.bgp.rib.impl.BGPHandlerFactory;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionImpl;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionNegotiatorFactory;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionProposalImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.ProtocolHandlerFactory;
import org.opendaylight.protocol.framework.ProtocolSession;
import org.opendaylight.protocol.framework.SessionListener;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.base.Preconditions;

public class BGPSpeakerMock<M, S extends ProtocolSession<M>, L extends SessionListener<M, ?, ?>> extends AbstractDispatcher<S, L> {

	private final SessionNegotiatorFactory<M, S, L> negotiatorFactory;
	private final ProtocolHandlerFactory<?> factory;

	public BGPSpeakerMock(final SessionNegotiatorFactory<M, S, L> negotiatorFactory, final ProtocolHandlerFactory<?> factory,
			final DefaultPromise<BGPSessionImpl> defaultPromise) {
		this.negotiatorFactory = Preconditions.checkNotNull(negotiatorFactory);
		this.factory = Preconditions.checkNotNull(factory);
	}

	public void createServer(final InetSocketAddress address, final SessionListenerFactory<L> listenerFactory) {
		super.createServer(address, new PipelineInitializer<S>() {

			@Override
			public void initializeChannel(final SocketChannel ch, final Promise<S> promise) {
				ch.pipeline().addLast(BGPSpeakerMock.this.factory.getDecoders());
				ch.pipeline().addLast("negotiator",
						BGPSpeakerMock.this.negotiatorFactory.getSessionNegotiator(listenerFactory, ch, promise));
				ch.pipeline().addLast(BGPSpeakerMock.this.factory.getEncoders());
			}
		});
	}

	public static void main(final String[] args) throws IOException {

		final SessionListenerFactory<BGPSessionListener> f = new SessionListenerFactory<BGPSessionListener>() {
			@Override
			public BGPSessionListener getSessionListener() {
				return new SpeakerSessionListener();
			}
		};

		final BGPSessionPreferences prefs = new BGPSessionProposalImpl((short) 90, 25, new Ipv4Address("127.0.0.2")).getProposal();

		final SessionNegotiatorFactory<Notification, BGPSessionImpl, BGPSessionListener> snf = new BGPSessionNegotiatorFactory(new HashedWheelTimer(), prefs);

		final BGPSpeakerMock<Notification, BGPSessionImpl, BGPSessionListener> mock = new BGPSpeakerMock<Notification, BGPSessionImpl, BGPSessionListener>(snf, new BGPHandlerFactory(SimpleBGPMessageFactory.INSTANCE), new DefaultPromise<BGPSessionImpl>(GlobalEventExecutor.INSTANCE));

		mock.createServer(new InetSocketAddress("127.0.0.2", 12345), f);
	}
}
