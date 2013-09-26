/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import io.netty.channel.socket.SocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;
import java.util.List;

import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.ProtocolHandlerFactory;
import org.opendaylight.protocol.framework.ProtocolSession;
import org.opendaylight.protocol.framework.SessionListener;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPHandlerFactory;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.protocol.pcep.tlv.NodeIdentifierTlv;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class PCCMock<M, S extends ProtocolSession<M>, L extends SessionListener<M, ?, ?>> extends
AbstractDispatcher<S, L> {

	private final SessionNegotiatorFactory<M, S, L> negotiatorFactory;
	private final ProtocolHandlerFactory<?> factory;

	public PCCMock(final SessionNegotiatorFactory<M, S, L> negotiatorFactory, final ProtocolHandlerFactory<?> factory,
			final DefaultPromise<PCEPSessionImpl> defaultPromise) {
		this.negotiatorFactory = Preconditions.checkNotNull(negotiatorFactory);
		this.factory = Preconditions.checkNotNull(factory);
	}

	@Override
	public void initializeChannel(final SocketChannel ch, final Promise<S> promise, final SessionListenerFactory<L> listenerFactory) {
		ch.pipeline().addLast(this.factory.getDecoders());
		ch.pipeline().addLast("negotiator", this.negotiatorFactory.getSessionNegotiator(listenerFactory, ch, promise));
		ch.pipeline().addLast(this.factory.getEncoders());
	}

	public static void main(final String[] args) throws Exception {
		final List<PCEPTlv> tlvs = Lists.newArrayList();
		tlvs.add(new NodeIdentifierTlv(new byte[] { (byte) 127, (byte) 2, (byte) 3, (byte) 7 }));

		final SessionNegotiatorFactory<PCEPMessage, PCEPSessionImpl, PCEPSessionListener> snf = new DefaultPCEPSessionNegotiatorFactory(new HashedWheelTimer(), new PCEPOpenObject(30, 120, 0, tlvs), 0);

		final PCCMock<PCEPMessage, PCEPSessionImpl, PCEPSessionListener> pcc = new PCCMock<>(snf, new PCEPHandlerFactory(), new DefaultPromise<PCEPSessionImpl>(GlobalEventExecutor.INSTANCE));

		pcc.createClient(new InetSocketAddress("127.0.0.3", 12345), new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 2000),
				new SessionListenerFactory<PCEPSessionListener>() {

			@Override
			public PCEPSessionListener getSessionListener() {
				return new SimpleSessionListener();
			}
		}).get();
	}
}
