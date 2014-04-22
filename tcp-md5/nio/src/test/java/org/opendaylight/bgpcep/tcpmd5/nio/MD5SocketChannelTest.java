/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.nio;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.bgpcep.tcpmd5.KeyAccess;
import org.opendaylight.bgpcep.tcpmd5.KeyAccessFactory;
import org.opendaylight.bgpcep.tcpmd5.KeyMapping;
import org.opendaylight.bgpcep.tcpmd5.MD5SocketOptions;

public class MD5SocketChannelTest {
	@Mock
	private KeyAccessFactory keyAccessFactory;
	@Mock
	private KeyAccess keyAccess;

	// FIXME: can we mock this?
	private SocketChannel channel;

	@Before
	public void setup() throws IOException {
		MockitoAnnotations.initMocks(this);

		channel = new SocketChannel(null) {

			@Override
			public SocketAddress getLocalAddress() throws IOException {
				return null;
			}

			@Override
			public <T> T getOption(final SocketOption<T> name) throws IOException {
				return null;
			}

			@Override
			public Set<SocketOption<?>> supportedOptions() {
				return null;
			}

			@Override
			public SocketChannel bind(final SocketAddress local) throws IOException {
				return null;
			}

			@Override
			public <T> SocketChannel setOption(final SocketOption<T> name, final T value)
					throws IOException {
				return null;
			}

			@Override
			public SocketChannel shutdownInput() throws IOException {
				return null;
			}

			@Override
			public SocketChannel shutdownOutput() throws IOException {
				return null;
			}

			@Override
			public Socket socket() {
				return null;
			}

			@Override
			public boolean isConnected() {
				return false;
			}

			@Override
			public boolean isConnectionPending() {
				return false;
			}

			@Override
			public boolean connect(final SocketAddress remote) throws IOException {
				return false;
			}

			@Override
			public boolean finishConnect() throws IOException {
				return false;
			}

			@Override
			public SocketAddress getRemoteAddress() throws IOException {
				return null;
			}

			@Override
			public int read(final ByteBuffer dst) throws IOException {
				return 0;
			}

			@Override
			public long read(final ByteBuffer[] dsts, final int offset, final int length)
					throws IOException {
				return 0;
			}

			@Override
			public int write(final ByteBuffer src) throws IOException {
				return 0;
			}

			@Override
			public long write(final ByteBuffer[] srcs, final int offset, final int length)
					throws IOException {
				return 0;
			}

			@Override
			protected void implCloseSelectableChannel() throws IOException {
			}

			@Override
			protected void implConfigureBlocking(final boolean block)
					throws IOException {
			}
		};

		Mockito.doReturn(keyAccess).when(keyAccessFactory).getKeyAccess(channel);
		Mockito.doReturn(null).when(keyAccess).getKeys();
		Mockito.doNothing().when(keyAccess).setKeys(any(KeyMapping.class));
	}

	@Test
	public void testCreate() throws IOException {
		try (final MD5SocketChannel sc = new MD5SocketChannel(channel, keyAccessFactory)) {

		}

		Mockito.verify(keyAccessFactory).getKeyAccess(channel);
	}

	@Test
	public void testGetKey() throws IOException {
		try (final MD5SocketChannel sc = new MD5SocketChannel(channel, keyAccessFactory)) {

			assertNull(sc.getOption(MD5SocketOptions.TCP_MD5SIG));
		}

		Mockito.verify(keyAccessFactory).getKeyAccess(channel);
		Mockito.verify(keyAccess).getKeys();
	}

	@Test
	public void testSetKey() throws IOException {
		final KeyMapping map = new KeyMapping();
		map.put(InetAddress.getLoopbackAddress(), new byte[] { 1, 2, 3 });

		try (final MD5SocketChannel sc = new MD5SocketChannel(channel, keyAccessFactory)) {
			assertSame(sc, sc.setOption(MD5SocketOptions.TCP_MD5SIG, map));
		}

		Mockito.verify(keyAccessFactory).getKeyAccess(channel);
		Mockito.verify(keyAccess).setKeys(map);
	}
}
