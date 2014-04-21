/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DummyKeyAccessFactoryTest {
	private ServerSocketChannel ssc;
	private KeyAccessFactory kaf;
	private SocketChannel sc;

	@Before
	public void setup() throws IOException {
		kaf = DummyKeyAccessFactory.getInstance();
		sc = SocketChannel.open();
		ssc = ServerSocketChannel.open();
	}

	@After
	public void tearDown() throws IOException {
		sc.close();
		ssc.close();
	}

	@Test
	public void testCanHandleChannelClass() {
		assertFalse(kaf.canHandleChannelClass(sc.getClass()));
		assertFalse(kaf.canHandleChannelClass(ssc.getClass()));
	}

	@Test(expected=NullPointerException.class)
	public void testNullCanHandleChannelClass() {
		assertFalse(kaf.canHandleChannelClass(null));
	}

	@Test
	public void testGetKeyAccess() {
		assertNull(kaf.getKeyAccess(sc));
		assertNull(kaf.getKeyAccess(ssc));
	}

	@Test(expected=NullPointerException.class)
	public void testNullGetKeyAccess() {
		assertNull(kaf.getKeyAccess(null));
	}
}
