/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectionKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SSLSelectionKey extends AbstractSelectionKey {
	private static final Logger logger = LoggerFactory.getLogger(SSLSelectionKey.class);
	private final SelectableChannel channel;
	private final Selector selector;
	private final SelectionKey key;
	private int ops = 0, readyOps = 0;

	SSLSelectionKey(final Selector selector, final SelectionKey key, final SelectableChannel channel) {
		this.selector = selector;
		this.channel = channel;
		this.key = key;
	}

	@Override
	public SelectableChannel channel() {
		return channel;
	}

	@Override
	public int interestOps() {
		return ops;
	}

	@Override
	public SelectionKey interestOps(final int ops) {
		this.ops = ops;
		return this;
	}

	@Override
	public int readyOps() {
		return readyOps;
	}

	@Override
	public Selector selector() {
		return selector;
	}

	void cancelSlave() {
		key.cancel();
	}

	void updateInterestOps() {
		int newOps = ops;

		if (channel instanceof SSLSocketChannel) {
			newOps = ((SSLSocketChannel)channel).computeInterestOps(ops);
		} else if (channel instanceof SSLServerSocketChannel) {
			newOps = ((SSLServerSocketChannel)channel).computeInterestOps(ops);
		}

		logger.trace("Updating interestOps to {} (before SSL={})", newOps, ops);

		// FIXME: is this check sufficient?
		if (key.isValid())
			key.interestOps(newOps);
	}

	boolean preselectReady() {
		final int newReadyOps;

		// FIXME: abstract out interface
		if (channel instanceof SSLSocketChannel) {
			final SSLSocketChannel sc = (SSLSocketChannel)channel;
			newReadyOps = sc.computeReadyOps();

			if (sc.hasParent()) {
				logger.trace("Child key, ready {}", newReadyOps);
				if ((newReadyOps & SelectionKey.OP_CONNECT) != 0) {
					try {
						if (sc.finishConnect()) {
							this.cancel();
							return true;
						} else
							logger.trace("finishConnect indicated non-connect after poll. Possible leak.");
					} catch (IOException e) {
						logger.trace("Failed to establish child socket", e);
						this.cancel();
					}
				}
				return false;
			}
		} else if (channel instanceof SSLServerSocketChannel) {
			newReadyOps = ((SSLServerSocketChannel)channel).computeReadyOps();
		} else
			newReadyOps = 0;

		logger.trace("Preselect: ready {} interest {} (A: {} R: {} W: {})",
				newReadyOps, ops, SelectionKey.OP_CONNECT, SelectionKey.OP_READ, SelectionKey.OP_WRITE);
		return (newReadyOps & ops) != 0;
	}

	boolean updateReadyOps() {
		int newReadyOps = 0;
		if (channel instanceof SSLServerSocketChannel) {
			newReadyOps = ((SSLServerSocketChannel)channel).computeReadyOps();
		} else if (channel instanceof SSLSocketChannel) {
			final SSLSocketChannel sc = (SSLSocketChannel)channel;

			// Do not report events for internal channels
			if (!sc.hasParent())
				newReadyOps = sc.computeReadyOps();
		} else {
			try {
				newReadyOps = key.readyOps();
			} catch (CancelledKeyException e) {
				logger.trace("Encountered cancelled key, ignoring", e);
			}
		}

		if (readyOps == newReadyOps)
			return false;

		logger.trace("Updating readyOps from {} to {}", readyOps, newReadyOps);
		readyOps = newReadyOps;
		return true;
	}
}

