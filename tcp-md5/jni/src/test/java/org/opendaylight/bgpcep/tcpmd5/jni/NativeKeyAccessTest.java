/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.jni;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.bgpcep.tcpmd5.KeyAccess;
import org.opendaylight.bgpcep.tcpmd5.KeyAccessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativeKeyAccessTest {
	private static final Logger LOG = LoggerFactory.getLogger(NativeKeyAccessTest.class);
	private static final byte[] KEY1 = new byte[] { 1 };
	private static final byte[] KEY2 = new byte[] { 2, 3 };

	private KeyAccessFactory factory;
	private Channel channel;

	private void testKeyManipulation(final Channel c) throws IOException {
		assertTrue(factory.canHandleChannelClass(c.getClass()));

		final KeyAccess ka = factory.getKeyAccess(c);
		assertNull(ka.getKey());

		LOG.debug("Setting key {}", KEY1);
		ka.setKey(KEY1);
		assertArrayEquals(KEY1, ka.getKey());

		LOG.debug("Setting key {}", KEY2);
		ka.setKey(KEY2);
		assertArrayEquals(KEY2, ka.getKey());

		LOG.debug("Deleting key");
		ka.setKey(null);
		assertNull(ka.getKey());
	}

	@Before
	public void initialize() throws IOException {
		factory = NativeKeyAccessFactory.getInstance();
		channel = SocketChannel.open();
	}

	@After
	public void shutdown() throws IOException {
		channel.close();
	}

	@Test
	public void testSocket() throws IOException {
		testKeyManipulation(channel);
	}

	@Test(expected=NullPointerException.class)
	public void testNullChannel() {
		factory.getKeyAccess(null);
	}

	@Test(expected=NullPointerException.class)
	public void testNullClass() {
		factory.canHandleChannelClass(null);
	}

	@Test
	public void testServerSocket() throws IOException {
		try (final ServerSocketChannel ssc = ServerSocketChannel.open()) {
			testKeyManipulation(ssc);
		}
	}

	@Test(expected=IOException.class)
	public void testDeleteKey() throws IOException {
		final KeyAccess ka = factory.getKeyAccess(channel);
		assertNull(ka.getKey());
		ka.setKey(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testShortKey() throws IOException {
		final KeyAccess ka = factory.getKeyAccess(channel);
		ka.setKey(new byte[0]);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testLongKey() throws IOException {
		final KeyAccess ka = factory.getKeyAccess(channel);
		ka.setKey(new byte[81]);
	}
}
