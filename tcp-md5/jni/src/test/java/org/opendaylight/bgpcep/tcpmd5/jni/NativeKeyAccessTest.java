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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.junit.Test;

public class NativeKeyAccessTest {
	private static final byte[] KEY1 = new byte[] { 1 };
	private static final byte[] KEY2 = new byte[] { 2, 3 };

	@Test
	public void testSocket() throws IOException {
		final SocketChannel sc = SocketChannel.open();

		assertTrue(NativeKeyAccess.isAvailableForClass(sc.getClass()));

		final KeyAccess ka = NativeKeyAccess.create(sc);
		assertNull(ka.getKey());

		ka.setKey(null);
		assertNull(ka.getKey());

		ka.setKey(KEY1);
		assertArrayEquals(KEY1, ka.getKey());

		ka.setKey(KEY2);
		assertArrayEquals(KEY2, ka.getKey());

		ka.setKey(null);
		assertNull(ka.getKey());
	}

	@Test
	public void testServerSocket() throws IOException {
		final ServerSocketChannel ssc = ServerSocketChannel.open();

		assertTrue(NativeKeyAccess.isAvailableForClass(ssc.getClass()));

		final KeyAccess ka = NativeKeyAccess.create(ssc);
		assertNull(ka.getKey());

		ka.setKey(null);
		assertNull(ka.getKey());

		ka.setKey(KEY1);
		assertArrayEquals(KEY1, ka.getKey());

		ka.setKey(KEY2);
		assertArrayEquals(KEY2, ka.getKey());
		ka.setKey(null);
		assertNull(ka.getKey());
	}
}
