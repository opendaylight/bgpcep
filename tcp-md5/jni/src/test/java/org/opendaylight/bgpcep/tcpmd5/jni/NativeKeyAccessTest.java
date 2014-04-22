/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.jni;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.bgpcep.tcpmd5.KeyAccess;
import org.opendaylight.bgpcep.tcpmd5.KeyAccessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public class NativeKeyAccessTest {
	private static final Logger LOG = LoggerFactory.getLogger(NativeKeyAccessTest.class);
	private static final byte[] KEY1 = new byte[] { 1 };
	private static final byte[] KEY2 = new byte[] { 2, 3 };

	private KeyAccessFactory factory;
	private Channel channel;

	private void testKeyManipulation(final Channel c) throws IOException {
		assertTrue(factory.canHandleChannelClass(c.getClass()));

		final KeyAccess ka = factory.getKeyAccess(c);
		assertEquals(Collections.emptyMap(), ka.getKeys());

		final Map<InetAddress, byte[]> key1 = ImmutableMap.<InetAddress, byte[]>builder().put(InetAddress.getLocalHost(), KEY1).build();
		LOG.debug("Setting key {}", KEY1);
		ka.setKeys(key1);
		assertEquals(key1, ka.getKeys());

		final Map<InetAddress, byte[]> key2 = ImmutableMap.<InetAddress, byte[]>builder().put(InetAddress.getLocalHost(), KEY2).build();
		LOG.debug("Setting key {}", KEY2);
		ka.setKeys(key2);
		assertEquals(key2, ka.getKeys());

		LOG.debug("Deleting keys");
		ka.setKeys(Collections.<InetAddress, byte[]>emptyMap());
		assertEquals(Collections.emptyMap(), ka.getKeys());
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

	@Test(expected=IllegalArgumentException.class)
	public void testShortKey() throws IOException {
		final KeyAccess ka = factory.getKeyAccess(channel);

		final Map<InetAddress, byte[]> map = ImmutableMap.<InetAddress, byte[]>builder().put(InetAddress.getLocalHost(), new byte[0]).build();
		ka.setKeys(map);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testLongKey() throws IOException {
		final KeyAccess ka = factory.getKeyAccess(channel);
		final Map<InetAddress, byte[]> map = ImmutableMap.<InetAddress, byte[]>builder().put(InetAddress.getLocalHost(), new byte[81]).build();
		ka.setKeys(map);
	}
}
