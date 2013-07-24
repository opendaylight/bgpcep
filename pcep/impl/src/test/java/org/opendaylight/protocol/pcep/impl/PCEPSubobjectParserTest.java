/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.concepts.ASNumber;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv4Prefix;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.concepts.IPv6Prefix;
import org.opendaylight.protocol.concepts.SharedRiskLinkGroup;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.concepts.UnnumberedInterfaceIdentifier;
import org.opendaylight.protocol.pcep.impl.subobject.EROAsNumberSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROIPv4PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROIPv6PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.subobject.EROAsNumberSubobject;
import org.opendaylight.protocol.pcep.subobject.EROExplicitExclusionRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.EROGeneralizedLabelSubobject;
import org.opendaylight.protocol.pcep.subobject.EROIPPrefixSubobject;
import org.opendaylight.protocol.pcep.subobject.EROPathKeyWith128PCEIDSubobject;
import org.opendaylight.protocol.pcep.subobject.EROPathKeyWith32PCEIDSubobject;
import org.opendaylight.protocol.pcep.subobject.EROProtectionType1Subobject;
import org.opendaylight.protocol.pcep.subobject.EROProtectionType2Subobject;
import org.opendaylight.protocol.pcep.subobject.EROType1LabelSubobject;
import org.opendaylight.protocol.pcep.subobject.EROUnnumberedInterfaceSubobject;
import org.opendaylight.protocol.pcep.subobject.EROWavebandSwitchingLabelSubobject;
import org.opendaylight.protocol.pcep.subobject.ExcludeRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.ExplicitRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.RROAttributesSubobject;
import org.opendaylight.protocol.pcep.subobject.RROGeneralizedLabelSubobject;
import org.opendaylight.protocol.pcep.subobject.RROIPAddressSubobject;
import org.opendaylight.protocol.pcep.subobject.RROPathKeyWith128PCEIDSubobject;
import org.opendaylight.protocol.pcep.subobject.RROPathKeyWith32PCEIDSubobject;
import org.opendaylight.protocol.pcep.subobject.RROProtectionType1Subobject;
import org.opendaylight.protocol.pcep.subobject.RROProtectionType2Subobject;
import org.opendaylight.protocol.pcep.subobject.RROType1LabelSubobject;
import org.opendaylight.protocol.pcep.subobject.RROWavebandSwitchingLabelSubobject;
import org.opendaylight.protocol.pcep.subobject.ReportedRouteSubobject;
import org.opendaylight.protocol.pcep.subobject.XROAsNumberSubobject;
import org.opendaylight.protocol.pcep.subobject.XROIPPrefixSubobject;
import org.opendaylight.protocol.pcep.subobject.XROSRLGSubobject;
import org.opendaylight.protocol.pcep.subobject.XROSubobjectAttribute;
import org.opendaylight.protocol.pcep.subobject.XROUnnumberedInterfaceSubobject;

/**
 * Tests for subobjects
 */
public class PCEPSubobjectParserTest {
    final byte[] ipv6bytes1 = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
	    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
    final byte[] ipv6bytes2 = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0x12, (byte) 0x34,
	    (byte) 0x54, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

    final byte[] ipv4bytes1 = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
    final byte[] ipv4bytes2 = { (byte) 0x12, (byte) 0x34, (byte) 0x50, (byte) 0x00 };

    @Test
    public void testSerDeser() throws PCEPDeserializerException, IOException {
	final byte[] bytesFromFile = ByteArray.fileToBytes("src/test/resources/PackOfSubobjects.bin");
	final List<ExplicitRouteSubobject> objsToTest = PCEPEROSubobjectParser.parse(bytesFromFile);

	assertEquals(8, objsToTest.size());

	assertEquals(objsToTest.get(0), new EROAsNumberSubobject(new ASNumber(0xFFFFL), true));
	assertEquals(objsToTest.get(1), new EROAsNumberSubobject(new ASNumber(0x0010L), false));
	assertEquals(objsToTest.get(2),
		new EROIPPrefixSubobject<IPv4Prefix>(new IPv4Prefix(new IPv4Address(this.ipv4bytes1), 0x20),
			true));

	assertEquals(objsToTest.get(3),
		new EROIPPrefixSubobject<IPv4Prefix>(new IPv4Prefix(new IPv4Address(this.ipv4bytes2), 0x15),
			false));
	assertEquals(objsToTest.get(4),
		new EROIPPrefixSubobject<IPv6Prefix>(new IPv6Prefix(new IPv6Address(this.ipv6bytes1), 0x80),
			true));

	assertEquals(objsToTest.get(5),
		new EROIPPrefixSubobject<IPv6Prefix>(new IPv6Prefix(new IPv6Address(this.ipv6bytes2), 0x16),
			false));
	final byte[] addr1 = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
	assertEquals(objsToTest.get(6), new EROUnnumberedInterfaceSubobject(new IPv4Address(addr1),
		new UnnumberedInterfaceIdentifier(0xFFFFFFFFL), true));

	final byte[] addr2 = { (byte) 0x01, (byte) 0x24, (byte) 0x56, (byte) 0x78 };
	assertEquals(objsToTest.get(7), new EROUnnumberedInterfaceSubobject(new IPv4Address(addr2),
		new UnnumberedInterfaceIdentifier(0x9ABCDEF0L), false));

	assertArrayEquals(bytesFromFile, PCEPEROSubobjectParser.put(objsToTest));

    }

    @Test
    public void testEROSubojectsSerDeserWithoutBin() throws PCEPDeserializerException {
	final List<ExplicitRouteSubobject> objsToTest = new ArrayList<ExplicitRouteSubobject>();
	objsToTest.add(new EROType1LabelSubobject(0xFFFF51F2L, true, false));
	objsToTest.add(new EROType1LabelSubobject(0x12345648L, false, true));
	objsToTest.add(new EROGeneralizedLabelSubobject(new byte[] { (byte) 0x12, (byte) 0x00, (byte) 0x25, (byte) 0xFF }, true, true));
	objsToTest.add(new EROWavebandSwitchingLabelSubobject(0x12345678L, 0x87654321L, 0xFFFFFFFFL, false, false));
	objsToTest.add(new EROProtectionType1Subobject(true, (byte) 0x05, true));
	objsToTest.add(new EROProtectionType2Subobject(true, false, true, true, (byte) 0x06, (byte) 0x3f, true, false, (byte) 0x00, false));
	objsToTest.add(new EROPathKeyWith32PCEIDSubobject(0x1235, new byte[] { (byte) 0x00, (byte) 0x55, (byte) 0xFF, (byte) 0xF1 }, true));
	objsToTest.add(new EROPathKeyWith128PCEIDSubobject(0x5432, new byte[] { (byte) 0x00, (byte) 0x55, (byte) 0xFF, (byte) 0xF1, (byte) 0x00, (byte) 0x55,
		(byte) 0xFF, (byte) 0xF1, (byte) 0x00, (byte) 0x55, (byte) 0xFF, (byte) 0xF1, (byte) 0x00, (byte) 0x55, (byte) 0xFF, (byte) 0xF1 }, true));
	objsToTest.add(new EROExplicitExclusionRouteSubobject(Arrays.asList((ExcludeRouteSubobject) new XROAsNumberSubobject(new ASNumber(2588), true))));

	assertEquals(objsToTest, PCEPEROSubobjectParser.parse(PCEPEROSubobjectParser.put(objsToTest)));
    }

    @Test
    public void testRROSubojectsSerDeserWithoutBin() throws PCEPDeserializerException {
	final List<ReportedRouteSubobject> objsToTest = new ArrayList<ReportedRouteSubobject>();
	objsToTest.add(new RROIPAddressSubobject<IPv6Prefix>(new IPv6Prefix(new IPv6Address(this.ipv6bytes2),
		0x16), true, false));
	objsToTest.add(new RROIPAddressSubobject<IPv4Prefix>(new IPv4Prefix(new IPv4Address(this.ipv4bytes1),
		0x16), true, false));
	objsToTest.add(new RROType1LabelSubobject(0xFFFF51F2L, true));
	objsToTest.add(new RROType1LabelSubobject(0x12345648L, false));
	objsToTest.add(new RROGeneralizedLabelSubobject(new byte[] { (byte) 0x12, (byte) 0x00, (byte) 0x25, (byte) 0xFF }, true));
	objsToTest.add(new RROWavebandSwitchingLabelSubobject(0x12345678L, 0x87654321L, 0xFFFFFFFFL, false));
	objsToTest.add(new RROProtectionType1Subobject(true, (byte) 0x05));
	objsToTest.add(new RROProtectionType2Subobject(true, false, true, true, (byte) 0x06, (byte) 0x3f, true, false, (byte) 0x00));
	objsToTest.add(new RROPathKeyWith32PCEIDSubobject(0x1235, new byte[] { (byte) 0x00, (byte) 0x55, (byte) 0xFF, (byte) 0xF1 }));
	objsToTest.add(new RROPathKeyWith128PCEIDSubobject(0x5432, new byte[] { (byte) 0x00, (byte) 0x55, (byte) 0xFF, (byte) 0xF1, (byte) 0x00, (byte) 0x55,
		(byte) 0xFF, (byte) 0xF1, (byte) 0x00, (byte) 0x55, (byte) 0xFF, (byte) 0xF1, (byte) 0x00, (byte) 0x55, (byte) 0xFF, (byte) 0xF1 }));
	objsToTest.add(new RROAttributesSubobject(new byte[] { (byte) 0x00, (byte) 0x55, (byte) 0xFF, (byte) 0xF1, (byte) 0x00, (byte) 0x55, (byte) 0xFF,
		(byte) 0xF1, (byte) 0x00, (byte) 0x55, (byte) 0xFF, (byte) 0xF1, (byte) 0x00, (byte) 0x55, (byte) 0xFF, (byte) 0xF1 }));

	assertEquals(objsToTest, PCEPRROSubobjectParser.parse(PCEPRROSubobjectParser.put(objsToTest)));
    }

    @Test
    public void testXROSubojectsSerDeserWithoutBin() throws PCEPDeserializerException {
	final List<ExcludeRouteSubobject> objsToTest = new ArrayList<ExcludeRouteSubobject>();
	objsToTest.add(new XROIPPrefixSubobject<IPv6Prefix>(new IPv6Prefix(new IPv6Address(this.ipv6bytes2),
		0x16), true, XROSubobjectAttribute.INTERFACE));
	objsToTest.add(new XROIPPrefixSubobject<IPv4Prefix>(new IPv4Prefix(new IPv4Address(this.ipv4bytes1),
		0x16), false, XROSubobjectAttribute.INTERFACE));
	objsToTest.add(new XROAsNumberSubobject(new ASNumber(0x1234), true));
	objsToTest.add(new XROUnnumberedInterfaceSubobject(new IPv4Address(this.ipv4bytes1),
		new UnnumberedInterfaceIdentifier(0xFFFFFFFFL), true, XROSubobjectAttribute.SRLG));
	objsToTest.add(new XROSRLGSubobject(new SharedRiskLinkGroup(0x12345678L), false));

	assertEquals(objsToTest, PCEPXROSubobjectParser.parse(PCEPXROSubobjectParser.put(objsToTest)));
    }

    @Test
    public void testDifferentLengthExceptions() {
	final byte[] bytes = { (byte) 0x00 }; // not empty but not enought data
					      // for parsing subobjects

	try {
	    EROAsNumberSubobjectParser.parse(bytes, true);
	    fail("");
	} catch (final PCEPDeserializerException e) {
	}

	try {
	    EROUnnumberedInterfaceSubobjectParser.parse(bytes, true);
	    fail("");
	} catch (final PCEPDeserializerException e) {
	}

	try {
	    EROIPv4PrefixSubobjectParser.parse(bytes, true);
	    fail("");
	} catch (final PCEPDeserializerException e) {
	}

	try {
	    EROIPv6PrefixSubobjectParser.parse(bytes, true);
	    fail("");
	} catch (final PCEPDeserializerException e) {
	}
    }

    @Test
    public void testNullExceptions() throws PCEPDeserializerException {
	final byte[] bytes = null; // not empty but not enought data for parsing
				   // subobjects

	try {
	    EROAsNumberSubobjectParser.parse(bytes, true);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    EROUnnumberedInterfaceSubobjectParser.parse(bytes, true);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    EROIPv4PrefixSubobjectParser.parse(bytes, true);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    EROIPv6PrefixSubobjectParser.parse(bytes, true);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}
    }

    @Test
    public void testUnknownInstanceExceptions() {

	final ExplicitRouteSubobject instance = new ExplicitRouteSubobject() {
	};

	try {
	    EROAsNumberSubobjectParser.put(instance);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    EROUnnumberedInterfaceSubobjectParser.put(instance);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    EROIPv4PrefixSubobjectParser.put(instance);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    final byte[] ipv6addr = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
	    EROIPv4PrefixSubobjectParser.put(new EROIPPrefixSubobject<IPv6Prefix>(new IPv6Prefix(new IPv6Address(ipv6addr), 1), false));
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    EROIPv6PrefixSubobjectParser.put(instance);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    final byte[] ipv4addr = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
	    EROIPv6PrefixSubobjectParser.put(new EROIPPrefixSubobject<IPv4Prefix>(new IPv4Prefix(new IPv4Address(ipv4addr), 1), false));
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

    }

    @Test
    public void testEmptyExceptions() throws PCEPDeserializerException {
	final byte[] bytes = {}; // not empty but not enought data for parsing
				 // subobjects

	try {
	    EROAsNumberSubobjectParser.parse(bytes, true);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    EROUnnumberedInterfaceSubobjectParser.parse(bytes, true);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    EROIPv4PrefixSubobjectParser.parse(bytes, true);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}

	try {
	    EROIPv6PrefixSubobjectParser.parse(bytes, true);
	    fail("");
	} catch (final IllegalArgumentException e) {
	}
    }

}
