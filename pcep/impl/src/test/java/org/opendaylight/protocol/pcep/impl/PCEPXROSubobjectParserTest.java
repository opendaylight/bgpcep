/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.impl.subobject.XROAsNumberSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROIpPrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.util.ByteArray;

public class PCEPXROSubobjectParserTest {

	@Test
	public void testSerDeser() throws PCEPDeserializerException, IOException {
		final byte[] bytesFromFile = ByteArray.fileToBytes("src/test/resources/PackOfXROSubobjects.bin");
		// final List<ExcludeRouteSubobject> objsToTest = PCEPXROSubobjectParser.parse(bytesFromFile);

		// assertEquals(5, objsToTest.size());
		//
		// assertEquals(
		// objsToTest.get(0),
		// new XROIPPrefixSubobject(new IpPrefix(Ipv4Util.prefixForBytes(new byte[] { (byte) 192, (byte) 168, (byte) 0,
		// (byte) 0 }, 16)), true, XROSubobjectAttribute.NODE));
		// assertEquals(
		// objsToTest.get(1),
		// new XROIPPrefixSubobject(new IpPrefix(Ipv6Util.prefixForBytes(new byte[] { (byte) 0x12, (byte) 0x34, (byte)
		// 0x56,
		// (byte) 0x78, (byte) 0x90, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x90, (byte) 0x12,
		// (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0, (byte) 0 }, 112)), true, XROSubobjectAttribute.INTERFACE));
		// assertEquals(objsToTest.get(2), new XROUnnumberedInterfaceSubobject(new IPv4Address(new byte[] { (byte) 0,
		// (byte) 0, (byte) 0,
		// (byte) 0x20 }), new UnnumberedInterfaceIdentifier(0x1234L), false, XROSubobjectAttribute.SRLG));
		// assertEquals(objsToTest.get(3), new XROAsNumberSubobject(new AsNumber((long) 0x1234), false));
		// assertEquals(objsToTest.get(4), new XROSRLGSubobject(new SharedRiskLinkGroup(0x12345678L), false));

		// assertArrayEquals(bytesFromFile, PCEPXROSubobjectParser.put(objsToTest));

	}

	@Test
	public void testDifferentLengthExceptions() {
		final byte[] bytes = { (byte) 0x00 }; // not empty but not enought data for parsing subobjects

		try {
			new XROAsNumberSubobjectParser().parseSubobject(bytes, true);
			fail("");
		} catch (final PCEPDeserializerException e) {
		}

		try {
			new XROUnnumberedInterfaceSubobjectParser().parseSubobject(bytes, true);
			fail("");
		} catch (final PCEPDeserializerException e) {
		}

		try {
			new XROIpPrefixSubobjectParser().parseSubobject(bytes, true);
			fail("");
		} catch (final PCEPDeserializerException e) {
		}
	}
}
