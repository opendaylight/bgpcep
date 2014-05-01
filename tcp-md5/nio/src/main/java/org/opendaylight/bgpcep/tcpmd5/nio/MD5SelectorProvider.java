/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.nio;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Map;
import java.util.WeakHashMap;

import org.opendaylight.bgpcep.tcpmd5.KeyAccessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public final class MD5SelectorProvider extends SelectorProvider {
	private static final Logger LOG = LoggerFactory.getLogger(MD5SelectorProvider.class);
	private static final Map<SelectorProvider, MD5SelectorProvider> INSTANCES = new WeakHashMap<>();
	private final KeyAccessFactory keyAccessFactory;
	private final SelectorProvider delegate;

	private MD5SelectorProvider(final KeyAccessFactory keyAccessFactory, final SelectorProvider delegate) {
		this.keyAccessFactory = Preconditions.checkNotNull(keyAccessFactory);
		this.delegate = Preconditions.checkNotNull(delegate);
	}

	public synchronized static MD5SelectorProvider getInstance(final KeyAccessFactory keyAccessFactory, final SelectorProvider provider) {
		MD5SelectorProvider ret = INSTANCES.get(provider);
		if (ret == null) {
			ret = new MD5SelectorProvider(keyAccessFactory, provider);
			LOG.debug("Created new provider instance {} for delegate {}", ret, provider);
			INSTANCES.put(provider, ret);
		}

		return ret;
	}

	public static MD5SelectorProvider getInstance(final KeyAccessFactory keyAccessFactory) {
		return getInstance(keyAccessFactory, provider());
	}

	@Override
	public DatagramChannel openDatagramChannel() throws IOException {
		throw new UnsupportedOperationException("Datagram channels are not supported");
	}

	@Override
	public DatagramChannel openDatagramChannel(final ProtocolFamily family) throws IOException {
		throw new UnsupportedOperationException("Datagram channels are not supported");
	}

	@Override
	public Pipe openPipe() throws IOException {
		throw new UnsupportedOperationException("Pipes are not supported");
	}

	@Override
	public AbstractSelector openSelector() throws IOException {
		final AbstractSelector s = delegate.openSelector();
		final AbstractSelector ret = new SelectorFacade(delegate, s);

		LOG.debug("Opened facade {} for selector {}", ret, s);
		return ret;
	}

	@Override
	public MD5ServerSocketChannel openServerSocketChannel() throws IOException {
		final ServerSocketChannel ch = delegate.openServerSocketChannel();
		final MD5ServerSocketChannel ret = new MD5ServerSocketChannel(ch, keyAccessFactory);

		LOG.debug("Created facade {} for server channel {}", ret, ch);
		return ret;
	}

	@Override
	public MD5SocketChannel openSocketChannel() throws IOException {
		final SocketChannel ch = delegate.openSocketChannel();
		final MD5SocketChannel ret = new MD5SocketChannel(ch, keyAccessFactory);

		LOG.debug("Created facade {} for channel {}", ret, ch);
		return ret;
	}
}
