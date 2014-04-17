/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.jni;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.junit.Test;

public class NativeKeyAccessTest {

	@Test
	public void testAvailability() throws IOException {
		final SocketChannel sc = SocketChannel.open();

		assertTrue(NativeKeyAccess.isAvailableForClass(sc.getClass()));
	}


	@Test
	public void testServerAvailability() throws IOException {
		final ServerSocketChannel ssc = ServerSocketChannel.open();

		assertTrue(NativeKeyAccess.isAvailableForClass(ssc.getClass()));
	}
}
