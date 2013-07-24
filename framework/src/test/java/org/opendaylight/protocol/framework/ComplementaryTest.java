/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import static org.junit.Assert.*;

import org.junit.Test;

public class ComplementaryTest {

	private class ProtocolMessageImpl implements ProtocolMessage {

		private static final long serialVersionUID = 1L;
	}

	private class ProtocolMessageFactoryImpl implements ProtocolMessageFactory {

		public ProtocolMessageFactoryImpl() {

		}

		@Override
		public ProtocolMessage parse(final byte[] bytes,
				final ProtocolMessageHeader msgHeader) throws DeserializerException,
				DocumentedException {
			return null;
		}

		@Override
		public byte[] put(final ProtocolMessage msg) {
			return new byte[]{ 12, 13 };
		}
	}

	@Test
	public void testExceptions() {
		final DeserializerException de = new DeserializerException("some error");
		final DocumentedException ee = new DocumentedException("some error");

		assertEquals(de.getMessage(), ee.getMessage());
	}

	@Test
	public void testProtocolOutputStream() {
		final ProtocolOutputStream pos = new ProtocolOutputStream();
		pos.putMessage(new ProtocolMessageImpl(), new ProtocolMessageFactoryImpl());
		try {
			pos.putMessage(new ProtocolMessageImpl(), new ProtocolMessageFactory() {

				@Override
				public byte[] put(final ProtocolMessage msg) {
					return null;
				}

				@Override
				public ProtocolMessage parse(final byte[] bytes, final ProtocolMessageHeader msgHeader)
						throws DeserializerException, DocumentedException {
					return null;
				}
			});
			fail("Exception should have occured.");
		} catch (final IllegalArgumentException e) {
			assertEquals("Message parsed to null.", e.getMessage());
			assertEquals(1, pos.getBuffers().size());
			assertArrayEquals(new byte[] { 12, 13}, pos.getBuffers().peek().array());
		}
	}
}
