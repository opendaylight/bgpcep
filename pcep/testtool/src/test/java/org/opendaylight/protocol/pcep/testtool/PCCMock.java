/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;

import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.protocol.framework.ProtocolHandlerFactory;
import org.opendaylight.protocol.framework.ProtocolSession;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.SessionListener;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.impl.DefaultPCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.impl.PCEPHandlerFactory;
import org.opendaylight.protocol.pcep.impl.PCEPSessionImpl;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.predundancy.group.id.tlv.PredundancyGroupIdBuilder;

import com.google.common.base.Preconditions;

public class PCCMock<M, S extends ProtocolSession<M>, L extends SessionListener<M, ?, ?>> extends AbstractDispatcher<S, L> {

	private final SessionNegotiatorFactory<M, S, L> negotiatorFactory;
	private final ProtocolHandlerFactory<?> factory;

	public PCCMock(final SessionNegotiatorFactory<M, S, L> negotiatorFactory, final ProtocolHandlerFactory<?> factory,
			final DefaultPromise<PCEPSessionImpl> defaultPromise) {
		super(new NioEventLoopGroup(), new NioEventLoopGroup());
		this.negotiatorFactory = Preconditions.checkNotNull(negotiatorFactory);
		this.factory = Preconditions.checkNotNull(factory);
	}

	public Future<S> createClient(final InetSocketAddress address, final ReconnectStrategy strategy,
			final SessionListenerFactory<L> listenerFactory) {
		return super.createClient(address, strategy, new PipelineInitializer<S>() {
			@Override
			public void initializeChannel(final SocketChannel ch, final Promise<S> promise) {
				ch.pipeline().addLast(PCCMock.this.factory.getDecoders());
				ch.pipeline().addLast("negotiator", PCCMock.this.negotiatorFactory.getSessionNegotiator(listenerFactory, ch, promise));
				ch.pipeline().addLast(PCCMock.this.factory.getEncoders());
			}
		});
	}

	public static void main(final String[] args) throws Exception {
		final TlvsBuilder builder = new TlvsBuilder();
		builder.setPredundancyGroupId(new PredundancyGroupIdBuilder().setIdentifier(new byte[] { (byte) 127, (byte) 2, (byte) 3, (byte) 7 }).build());

		final SessionNegotiatorFactory<Message, PCEPSessionImpl, PCEPSessionListener> snf = new DefaultPCEPSessionNegotiatorFactory(new HashedWheelTimer(), new OpenBuilder().setKeepalive(
				(short) 30).setDeadTimer((short) 120).setSessionId((short) 0).setTlvs(builder.build()).build(), 0);

		final PCCMock<Message, PCEPSessionImpl, PCEPSessionListener> pcc = new PCCMock<>(snf, new PCEPHandlerFactory(ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry()), new DefaultPromise<PCEPSessionImpl>(GlobalEventExecutor.INSTANCE));

		pcc.createClient(new InetSocketAddress("127.0.0.3", 12345), new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 2000),
				new SessionListenerFactory<PCEPSessionListener>() {

			@Override
			public PCEPSessionListener getSessionListener() {
				return new SimpleSessionListener();
			}
		}).get();
	}
}
