/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.nio;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

import com.google.common.base.Preconditions;

final class SelectorFacade extends AbstractSelector {
	private final AbstractSelector delegate;

	SelectorFacade(final SelectorProvider provider, final AbstractSelector delegate) {
		super(provider);
		this.delegate = Preconditions.checkNotNull(delegate);
	}

	@Override
	protected void implCloseSelector() throws IOException {
		delegate.close();
	}

	@Override
	protected SelectionKey register(final AbstractSelectableChannel ch, final int ops, final Object att) {
		Preconditions.checkArgument(ch instanceof ProxyChannel<?>);

		final AbstractSelectableChannel ach = ((ProxyChannel<?>)ch).getDelegate();
		try {
			return ach.register(delegate, ops, att);
		} catch (ClosedChannelException e) {
			throw new IllegalArgumentException("Failed to register channel", e);
		}
	}

	@Override
	public Set<SelectionKey> keys() {
		return delegate.keys();
	}

	@Override
	public Set<SelectionKey> selectedKeys() {
		return delegate.selectedKeys();
	}

	@Override
	public int selectNow() throws IOException {
		return delegate.selectNow();
	}

	@Override
	public int select(final long timeout) throws IOException {
		return delegate.select(timeout);
	}

	@Override
	public int select() throws IOException {
		return delegate.select();
	}

	@Override
	public Selector wakeup() {
		return delegate.wakeup();
	}
}
