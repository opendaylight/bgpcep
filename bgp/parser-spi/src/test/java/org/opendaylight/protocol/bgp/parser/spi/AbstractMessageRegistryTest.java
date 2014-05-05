/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.KeepaliveBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class AbstractMessageRegistryTest {

	public static final byte[] keepAliveBMsg = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0x00, (byte) 0x13, (byte) 0x04 };

	private final AbstractMessageRegistry registry = new AbstractMessageRegistry() {

		@Override
		protected ByteBuf serializeMessageImpl(Notification message) {
			return Unpooled.copiedBuffer(keepAliveBMsg);
		}

		@Override
		protected Notification parseBody(int type, ByteBuf body, int messageLength) throws BGPDocumentedException {
			return new KeepaliveBuilder().build();
		}
	};

	@Test
	public void testRegistry() throws BGPDocumentedException, BGPParsingException {
		final Notification keepAlive = new KeepaliveBuilder().build();
		final ByteBuf serialized = this.registry.serializeMessage(keepAlive);
		assertArrayEquals(keepAliveBMsg, serialized.array());

		final Notification not = this.registry.parseMessage(Unpooled.copiedBuffer(keepAliveBMsg));
		assertTrue(not instanceof Keepalive);
	}
}
