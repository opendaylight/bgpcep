/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import static org.junit.Assert.*;

import org.junit.Test;

import org.opendaylight.protocol.util.ByteArray;
import com.google.common.base.Objects.ToStringHelper;

public class AbstractIdentifierTest {

	private class AbstractIdentifierT extends AbstractIdentifier<AbstractIdentifierT> {

		private static final long serialVersionUID = 3803643153695225193L;

		public byte[] bytes;

		public AbstractIdentifierT(final byte[] bytes) {
			this.bytes = bytes;
		}

		@Override
		protected byte[] getBytes() {
			return this.bytes;
		}

		@Override
		protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
			return toStringHelper.add("bytes", ByteArray.toHexString(bytes, "."));
		}
	}

	@Test
	public void testToString() {
		final AbstractIdentifier<AbstractIdentifierT> ai = new AbstractIdentifierT(new byte[] {(byte) 172, (byte) 168, 31, 8});

		assertEquals("AbstractIdentifierT{bytes=ac.a8.1f.08}", ai.toString());
	}


	@Test
	public void testCompareTo() {
		final AbstractIdentifier<AbstractIdentifierT> a1 = new AbstractIdentifierT(new byte[] {(byte) 172, (byte) 168, 31, 8});
		final AbstractIdentifier<AbstractIdentifierT> a2 = new AbstractIdentifierT(new byte[] {(byte) 172, (byte) 167, 31, 8});
		final AbstractIdentifier<AbstractIdentifierT> a4 = new AbstractIdentifierT(new byte[] {(byte) 172, (byte) 167});

		assertEquals(0, a1.compareTo((AbstractIdentifierT) a1));
		assertEquals(-1, a2.compareTo((AbstractIdentifierT) a1));
		assertEquals(1, a1.compareTo((AbstractIdentifierT) a2));

		final AbstractIdentifier<AbstractIdentifier<?>> a3 = new AbstractIdentifier<AbstractIdentifier<?>>() {

			private static final long serialVersionUID = 1L;

			@Override
			protected byte[] getBytes() {
				return null;
			}

			@Override
			protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
				return toStringHelper;
			}
		};

		try {
			a3.compareTo(a1);
			fail("Exception should have occured.");
		} catch (final IllegalArgumentException e) {
			assertEquals("Object " + a1 + " is not assignable to " + a3.getClass() , e.getMessage());
		}

		assertEquals(2, a2.compareTo((AbstractIdentifierT) a4));
	}

	@Test
	public void testHashCodeEquals() {
		final AbstractIdentifier<AbstractIdentifierT> a1 = new AbstractIdentifierT(new byte[] {(byte) 172, (byte) 168, 31, 8});
		final AbstractIdentifier<AbstractIdentifierT> a2 = new AbstractIdentifierT(new byte[] {(byte) 172, (byte) 167, 31, 8});

		assertNotSame(a1.hashCode(), a2.hashCode());
		assertFalse(a1.equals(a2));
		assertEquals(a1, a1);
	}
}
