/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv4Prefix;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.concepts.IPv6Prefix;
import org.opendaylight.protocol.concepts.SharedRiskLinkGroup;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.impl.subobject.XROAsNumberSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROIPv4PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROIPv6PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.subobject.ExcludeRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.XROAsNumberSubobject;
import org.opendaylight.protocol.pcep.subobject.XROIPPrefixSubobject;
import org.opendaylight.protocol.pcep.subobject.XROSRLGSubobject;
import org.opendaylight.protocol.pcep.subobject.XROSubobjectAttribute;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

public class PCEPXROSubobjectParserTest {

	@Test
	public void testSerDeser() throws PCEPDeserializerException, IOException {
		final byte[] bytesFromFile = ByteArray.fileToBytes("src/test/resources/PackOfXROSubobjects.bin");
		final List<ExcludeRouteSubobject> objsToTest = PCEPXROSubobjectParser.parse(bytesFromFile);

		assertEquals(5, objsToTest.size());

		assertEquals(objsToTest.get(0), new XROIPPrefixSubobject<IPv4Prefix>(new IPv4Prefix(new IPv4Address(new byte[] { (byte) 192,
				(byte) 168, (byte) 0, (byte) 0 }), 16), true, XROSubobjectAttribute.NODE));
		assertEquals(objsToTest.get(1), new XROIPPrefixSubobject<IPv6Prefix>(new IPv6Prefix(new IPv6Address(new byte[] { (byte) 0x12,
				(byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x90, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x90,
				(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0, (byte) 0 }), 112), true, XROSubobjectAttribute.INTERFACE));
		// assertEquals(objsToTest.get(2), new XROUnnumberedInterfaceSubobject(new IPv4Address(new byte[] { (byte) 0,
		// (byte) 0, (byte) 0,
		// (byte) 0x20 }), new UnnumberedInterfaceIdentifier(0x1234L), false, XROSubobjectAttribute.SRLG));
		assertEquals(objsToTest.get(3), new XROAsNumberSubobject(new AsNumber((long) 0x1234), false));
		assertEquals(objsToTest.get(4), new XROSRLGSubobject(new SharedRiskLinkGroup(0x12345678L), false));

		// assertArrayEquals(bytesFromFile, PCEPXROSubobjectParser.put(objsToTest));

	}

	@Test
	public void testDifferentLengthExceptions() {
		final byte[] bytes = { (byte) 0x00 }; // not empty but not enought data for parsing subobjects

		try {
			XROAsNumberSubobjectParser.parse(bytes, true);
			fail("");
		} catch (final PCEPDeserializerException e) {
		}

		try {
			XROUnnumberedInterfaceSubobjectParser.parse(bytes, true);
			fail("");
		} catch (final PCEPDeserializerException e) {
		}

		try {
			XROIPv4PrefixSubobjectParser.parse(bytes, true);
			fail("");
		} catch (final PCEPDeserializerException e) {
		}

		try {
			XROIPv6PrefixSubobjectParser.parse(bytes, true);
			fail("");
		} catch (final PCEPDeserializerException e) {
		}
	}

	@Test
	public void testNullExceptions() throws PCEPDeserializerException {
		final byte[] bytes = null; // not empty but not enought data for parsing subobjects

		try {
			XROAsNumberSubobjectParser.parse(bytes, true);
			fail("");
		} catch (final IllegalArgumentException e) {
		}

		try {
			XROUnnumberedInterfaceSubobjectParser.parse(bytes, true);
			fail("");
		} catch (final IllegalArgumentException e) {
		}

		try {
			XROIPv4PrefixSubobjectParser.parse(bytes, true);
			fail("");
		} catch (final IllegalArgumentException e) {
		}

		try {
			XROIPv6PrefixSubobjectParser.parse(bytes, true);
			fail("");
		} catch (final IllegalArgumentException e) {
		}
	}

	@Test
	public void testUnknownInstanceExceptions() {

		final ExcludeRouteSubobject instance = new ExcludeRouteSubobject(true) {
		};

		try {
			XROAsNumberSubobjectParser.put(instance);
			fail("");
		} catch (final IllegalArgumentException e) {
		}

		try {
			XROUnnumberedInterfaceSubobjectParser.put(instance);
			fail("");
		} catch (final IllegalArgumentException e) {
		}

		try {
			XROIPv4PrefixSubobjectParser.put(instance);
			fail("");
		} catch (final IllegalArgumentException e) {
		}

		try {
			final byte[] ipv6addr = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
			XROIPv4PrefixSubobjectParser.put(new XROIPPrefixSubobject<IPv6Prefix>(new IPv6Prefix(new IPv6Address(ipv6addr), 1), false, XROSubobjectAttribute.INTERFACE));
			fail("");
		} catch (final IllegalArgumentException e) {
		}

		try {
			XROIPv6PrefixSubobjectParser.put(instance);
			fail("");
		} catch (final IllegalArgumentException e) {
		}

		try {
			final byte[] ipv4addr = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
			XROIPv6PrefixSubobjectParser.put(new XROIPPrefixSubobject<IPv4Prefix>(new IPv4Prefix(new IPv4Address(ipv4addr), 1), false, XROSubobjectAttribute.INTERFACE));
			fail("");
		} catch (final IllegalArgumentException e) {
		}

	}

	@Test
	public void testEmptyExceptions() throws PCEPDeserializerException {
		final byte[] bytes = {}; // not empty but not enought data for parsing subobjects

		try {
			XROAsNumberSubobjectParser.parse(bytes, true);
			fail("");
		} catch (final IllegalArgumentException e) {
		}

		try {
			XROUnnumberedInterfaceSubobjectParser.parse(bytes, true);
			fail("");
		} catch (final IllegalArgumentException e) {
		}

		try {
			XROIPv4PrefixSubobjectParser.parse(bytes, true);
			fail("");
		} catch (final IllegalArgumentException e) {
		}

		try {
			XROIPv6PrefixSubobjectParser.parse(bytes, true);
			fail("");
		} catch (final IllegalArgumentException e) {
		}
	}

}
