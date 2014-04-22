/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.nio;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.NetworkChannel;
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

import com.google.common.collect.ImmutableSet;

public class MD5ChannelOptionsTest {

	@Mock
	private KeyAccessFactory keyAccessFactory;
	@Mock
	private KeyAccess keyAccess;
	@Mock
	private NetworkChannel channel;

	@Before
	public void setup() throws IOException {
		MockitoAnnotations.initMocks(this);

		Mockito.doReturn(keyAccess).when(keyAccessFactory).getKeyAccess(channel);
		Mockito.doReturn(null).when(keyAccess).getKeys();
		Mockito.doNothing().when(keyAccess).setKeys(any(KeyMapping.class));

		Mockito.doReturn(ImmutableSet.of(StandardSocketOptions.TCP_NODELAY)).when(channel).supportedOptions();
		Mockito.doReturn(false).when(channel).getOption(StandardSocketOptions.TCP_NODELAY);
		Mockito.doReturn(channel).when(channel).setOption(StandardSocketOptions.TCP_NODELAY, true);
	}

	@Test
	public void testCreate() {
		final MD5ChannelOptions opts = MD5ChannelOptions.create(keyAccessFactory, channel);

		final Set<SocketOption<?>> so = opts.supportedOptions();
		assertTrue(so.contains(MD5SocketOptions.TCP_MD5SIG));
		assertTrue(so.contains(StandardSocketOptions.TCP_NODELAY));

		Mockito.verifyZeroInteractions(keyAccess);
		Mockito.verify(keyAccessFactory).getKeyAccess(channel);
		Mockito.verify(channel).supportedOptions();
	}

	@Test
	public void testGetOption() throws IOException {
		final MD5ChannelOptions opts = MD5ChannelOptions.create(keyAccessFactory, channel);

		assertNull(opts.getOption(MD5SocketOptions.TCP_MD5SIG));
		assertFalse(opts.getOption(StandardSocketOptions.TCP_NODELAY));

		Mockito.verify(keyAccess).getKeys();
		Mockito.verify(channel).getOption(StandardSocketOptions.TCP_NODELAY);
	}

	@Test
	public void testSetOption() throws IOException {
		final MD5ChannelOptions opts = MD5ChannelOptions.create(keyAccessFactory, channel);

		final KeyMapping map = new KeyMapping();
		map.put(InetAddress.getLoopbackAddress(), new byte[] { 1, });

		opts.setOption(MD5SocketOptions.TCP_MD5SIG, map);
		opts.setOption(StandardSocketOptions.TCP_NODELAY, true);

		Mockito.verify(keyAccess).setKeys(any(KeyMapping.class));
		Mockito.verify(channel).setOption(StandardSocketOptions.TCP_NODELAY, true);
	}
}
