/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Promise;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

import com.google.common.base.Preconditions;

/**
 * Implementation of PCEPDispatcher.
 */
public class PCEPDispatcherImpl extends AbstractDispatcher<PCEPSessionImpl, PCEPSessionListener> implements PCEPDispatcher {

	private final SessionNegotiatorFactory<Message, PCEPSessionImpl, PCEPSessionListener> snf;

	private final PCEPHandlerFactory hf = new PCEPHandlerFactory();

	/**
	 * Creates an instance of PCEPDispatcherImpl, gets the default selector and opens it.
	 * 
	 * @throws IOException if some error occurred during opening the selector
	 */
	public PCEPDispatcherImpl(final SessionNegotiatorFactory<Message, PCEPSessionImpl, PCEPSessionListener> negotiatorFactory) {
		super();
		this.snf = Preconditions.checkNotNull(negotiatorFactory);
	}

	@Override
	public ChannelFuture createServer(final InetSocketAddress address, final SessionListenerFactory<PCEPSessionListener> listenerFactory) {
		return super.createServer(address, new PipelineInitializer<PCEPSessionImpl>() {
			@Override
			public void initializeChannel(final SocketChannel ch, final Promise<PCEPSessionImpl> promise) {
				ch.pipeline().addLast(PCEPDispatcherImpl.this.hf.getDecoders());
				ch.pipeline().addLast("negotiator", PCEPDispatcherImpl.this.snf.getSessionNegotiator(listenerFactory, ch, promise));
				ch.pipeline().addLast(PCEPDispatcherImpl.this.hf.getEncoders());
			}
		});
	}
}
