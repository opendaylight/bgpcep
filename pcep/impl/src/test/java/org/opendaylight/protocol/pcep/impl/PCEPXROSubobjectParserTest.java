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

import org.junit.Test;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.impl.subobject.XROAsNumberSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROIpPrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROPathKeySubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROSRLGSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobjects.subobject.type.PathKeyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.ExcludeRouteSubobjects.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.SrlgId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.SrlgBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.UnnumberedBuilder;

public class PCEPXROSubobjectParserTest {

	private static final byte[] ip4PrefixBytes = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x16, (byte) 0x00 };
	private static final byte[] ip6PrefixBytes = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0x16, (byte) 0x01 };
	private static final byte[] srlgBytes = { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x02 };
	private static final byte[] unnumberedBytes = { (byte) 0x00, (byte) 0x01, (byte) 0x12, (byte) 0x34, (byte) 0x50, (byte) 0x00,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
	private static final byte[] asNumberBytes = { (byte) 0x00, (byte) 0x64 };
	private static final byte[] pathKey32Bytes = { (byte) 0x12, (byte) 0x34, (byte) 0x12, (byte) 0x34, (byte) 0x50, (byte) 0x00 };
	private static final byte[] pathKey128Bytes = { (byte) 0x12, (byte) 0x34, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
			(byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0x12, (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00 };

	@Test
	public void testXROIp4PrefixSubobject() throws PCEPDeserializerException {
		final XROIpPrefixSubobjectParser parser = new XROIpPrefixSubobjectParser();
		final SubobjectsBuilder subs = new SubobjectsBuilder();
		subs.setMandatory(false);
		subs.setAttribute(Attribute.Interface);
		subs.setSubobjectType(new IpPrefixBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix("255.255.255.255/22"))).build());
		assertEquals(subs.build(), parser.parseSubobject(ip4PrefixBytes, false));
		assertArrayEquals(ip4PrefixBytes, parser.serializeSubobject(subs.build()));
	}

	@Test
	public void testXROIp6PrefixSubobject() throws PCEPDeserializerException {
		final XROIpPrefixSubobjectParser parser = new XROIpPrefixSubobjectParser();
		final SubobjectsBuilder subs = new SubobjectsBuilder();
		subs.setMandatory(true);
		subs.setAttribute(Attribute.Node);
		subs.setSubobjectType(new IpPrefixBuilder().setIpPrefix(
				new IpPrefix(Ipv6Util.prefixForBytes(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
						(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
						(byte) 0xFF, (byte) 0xFF, (byte) 0xFF }, 22))).build());
		assertEquals(subs.build(), parser.parseSubobject(ip6PrefixBytes, true));
		assertArrayEquals(ip6PrefixBytes, parser.serializeSubobject(subs.build()));
	}

	@Test
	public void testXROSrlgSubobject() throws PCEPDeserializerException {
		final XROSRLGSubobjectParser parser = new XROSRLGSubobjectParser();
		final SubobjectsBuilder subs = new SubobjectsBuilder();
		subs.setMandatory(true);
		subs.setAttribute(Attribute.Srlg);
		subs.setSubobjectType(new SrlgBuilder().setSrlgId(new SrlgId(0x12345678L)).build());
		assertEquals(subs.build(), parser.parseSubobject(srlgBytes, true));
		assertArrayEquals(srlgBytes, parser.serializeSubobject(subs.build()));
	}

	@Test
	public void testXROUnnumberedSubobject() throws PCEPDeserializerException {
		final XROUnnumberedInterfaceSubobjectParser parser = new XROUnnumberedInterfaceSubobjectParser();
		final SubobjectsBuilder subs = new SubobjectsBuilder();
		subs.setMandatory(true);
		subs.setAttribute(Attribute.Node);
		subs.setSubobjectType(new UnnumberedBuilder().setRouterId(0x12345000L).setInterfaceId(0xffffffffL).build());
		assertEquals(subs.build(), parser.parseSubobject(unnumberedBytes, true));
		assertArrayEquals(unnumberedBytes, parser.serializeSubobject(subs.build()));
	}

	@Test
	public void testXROAsNumberSubobject() throws PCEPDeserializerException {
		final XROAsNumberSubobjectParser parser = new XROAsNumberSubobjectParser();
		final SubobjectsBuilder subs = new SubobjectsBuilder();
		subs.setMandatory(true);
		subs.setSubobjectType(new AsNumberBuilder().setAsNumber(new AsNumber(0x64L)).build());
		assertEquals(subs.build(), parser.parseSubobject(asNumberBytes, true));
		assertArrayEquals(asNumberBytes, parser.serializeSubobject(subs.build()));
	}

	@Test
	public void testXROPathKey32Subobject() throws PCEPDeserializerException {
		final XROPathKeySubobjectParser parser = new XROPathKeySubobjectParser();
		final SubobjectsBuilder subs = new SubobjectsBuilder();
		subs.setMandatory(true);
		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobjects.subobject.type.path.key.PathKeyBuilder pBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobjects.subobject.type.path.key.PathKeyBuilder();
		pBuilder.setPceId(new PceId(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x50, (byte) 0x00 }));
		pBuilder.setPathKey(new PathKey(4660));
		subs.setSubobjectType(new PathKeyBuilder().setPathKey(pBuilder.build()).build());
		assertEquals(subs.build(), parser.parseSubobject(pathKey32Bytes, true));
		assertArrayEquals(pathKey32Bytes, parser.serializeSubobject(subs.build()));
	}

	@Test
	public void testXROPathKey128Subobject() throws PCEPDeserializerException {
		final XROPathKeySubobjectParser parser = new XROPathKeySubobjectParser();
		final SubobjectsBuilder subs = new SubobjectsBuilder();
		subs.setMandatory(true);
		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobjects.subobject.type.path.key.PathKeyBuilder pBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobjects.subobject.type.path.key.PathKeyBuilder();
		pBuilder.setPceId(new PceId(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE,
				(byte) 0x12, (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 }));
		pBuilder.setPathKey(new PathKey(4660));
		subs.setSubobjectType(new PathKeyBuilder().setPathKey(pBuilder.build()).build());
		assertEquals(subs.build(), parser.parseSubobject(pathKey128Bytes, true));
		assertArrayEquals(pathKey128Bytes, parser.serializeSubobject(subs.build()));
	}
}
