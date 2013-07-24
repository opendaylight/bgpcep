/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.protocol.util.RemoveOnlySet;
import com.google.common.collect.Sets;

class SSLSelector extends AbstractSelector {
	private static final Logger logger = LoggerFactory.getLogger(SSLSelector.class);
	private final Set<SelectionKey> selectedKeys = Sets.newHashSet();
	private final Set<SelectionKey> keys = Sets.newCopyOnWriteArraySet();

	private final Set<SelectionKey> guardedSelectedKeys = RemoveOnlySet.wrap(selectedKeys);
	private final Set<SelectionKey> guardedKeys = Collections.unmodifiableSet(keys);
	private final Selector selector;
	private boolean closed = false;

	SSLSelector(final Selector selector) throws IOException {
		super(selector.provider());
		this.selector = selector;
	}

	@Override
	protected void implCloseSelector() throws IOException {
		// Make sure selection won't block
		selector.wakeup();

		synchronized (this) {
			if (!closed) {
				closed = true;
				for (SelectionKey k : keys)
					k.cancel();

				keys.clear();
				selector.close();
			}
		}
	}

	@Override
	protected synchronized SelectionKey register(final AbstractSelectableChannel ch, final int ops, final Object att) {
		ensureOpen();

		final AbstractSelectableChannel slave;
		if (ch instanceof SSLServerSocketChannel)
			slave = ((SSLServerSocketChannel)ch).channel;
		else if (ch instanceof SSLSocketChannel)
			slave = ((SSLSocketChannel)ch).channel;
		else
			slave = ch;

		logger.trace("Register channel {} slave {} with ops {}", ch, slave, ops);

		final SelectionKey key;
		try {
			key = new SSLSelectionKey(this, slave.register(selector, 0, null), ch);
		} catch (ClosedChannelException e) {
			throw new IllegalStateException("Slave selector found the channel closed", e);
		}
		key.interestOps(ops);
		key.attach(att);
		keys.add(key);
		return key;
	}

	@Override
	public synchronized Set<SelectionKey> keys() {
		ensureOpen();
		return guardedKeys;
	}

	private void ensureOpen() {
		if (closed)
			throw new ClosedSelectorException();
	}

	private int afterSelect() {
		logger.trace("Running afterSelect");
		int ret = 0;

		final Set<SelectionKey> ck = cancelledKeys();
		synchronized (ck) {
			selectedKeys.removeAll(ck);

			for (final SelectionKey k : keys) {
				final boolean updated = ((SSLSelectionKey)k).updateReadyOps();
				if ((k.readyOps() & k.interestOps()) != 0) {
					selectedKeys.add(k);
					if (updated)
						++ret;
				} else
					selectedKeys.remove(k);
			}
		}

		return ret;
	}

	private boolean beforeSelect() {
		logger.trace("Running beforeSelect");

		final Set<SelectionKey> ck = cancelledKeys();
		synchronized (ck) {
			for (final SelectionKey k : ck)
				((SSLSelectionKey)k).cancelSlave();
			selectedKeys.removeAll(ck);
			keys.removeAll(ck);
			ck.clear();

			for (final SelectionKey k : keys) {
				final SSLSelectionKey sk = (SSLSelectionKey)k;
				if (sk.preselectReady()) {
					logger.trace("Key {} ready in preselect", k);
					return true;
				} else
					sk.updateInterestOps();
			}
		}

		return false;
	}

	@Override
	public synchronized int select() throws IOException {
		return select(0);
	}

	@Override
	public synchronized int select(final long timeout) throws IOException {
		ensureOpen();

		if (!beforeSelect()) {
			try {
				begin();
				selector.select(timeout);
			} finally {
				end();
			}
		}
		return afterSelect();
	}

	@Override
	public synchronized int selectNow() throws IOException {
		ensureOpen();

		if (!beforeSelect())
			selector.selectNow();
		return afterSelect();
	}

	@Override
	public synchronized Set<SelectionKey> selectedKeys() {
		ensureOpen();
		return guardedSelectedKeys;
	}

	@Override
	public Selector wakeup() {
		logger.trace("Running wakeup");
		selector.wakeup();
		return this;
	}
}
