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
import org.opendaylight.protocol.pcep.impl.subobject.RROIpPrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROLabelSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROPathKeySubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.subobjects.subobject.type.PathKeyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.GeneralizedLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.LabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.UnnumberedBuilder;

public class PCEPRROSubobjectParserTest {

	private static final byte[] ip4PrefixBytes = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x16, (byte) 0x01 };
	private static final byte[] ip6PrefixBytes = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
		(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
		(byte) 0xFF, (byte) 0x16, (byte) 0x02 };
	private static final byte[] unnumberedBytes = { (byte) 0x02, (byte) 0x00, (byte) 0x12, (byte) 0x34, (byte) 0x50, (byte) 0x00,
		(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
	private static final byte[] pathKey32Bytes = { (byte) 0x12, (byte) 0x34, (byte) 0x12, (byte) 0x34, (byte) 0x50, (byte) 0x00 };
	private static final byte[] pathKey128Bytes = { (byte) 0x12, (byte) 0x34, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
		(byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0x12, (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x00, (byte) 0x00, (byte) 0x00 };
	private static final byte[] labelBytes = { (byte) 0x81, (byte) 0x02, (byte) 0x12, (byte) 0x00, (byte) 0x25, (byte) 0xFF };

	@Test
	public void testRROIp4PrefixSubobject() throws PCEPDeserializerException {
		final RROIpPrefixSubobjectParser parser = new RROIpPrefixSubobjectParser();
		final SubobjectsBuilder subs = new SubobjectsBuilder();
		subs.setProtectionAvailable(true);
		subs.setProtectionInUse(false);
		subs.setSubobjectType(new IpPrefixBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix("255.255.255.255/22"))).build());
		assertEquals(subs.build(), parser.parseSubobject(ip4PrefixBytes));
		assertArrayEquals(ip4PrefixBytes, parser.serializeSubobject(subs.build()));
	}

	@Test
	public void testRROIp6PrefixSubobject() throws PCEPDeserializerException {
		final RROIpPrefixSubobjectParser parser = new RROIpPrefixSubobjectParser();
		final SubobjectsBuilder subs = new SubobjectsBuilder();
		subs.setProtectionAvailable(false);
		subs.setProtectionInUse(true);
		subs.setSubobjectType(new IpPrefixBuilder().setIpPrefix(
				new IpPrefix(Ipv6Util.prefixForBytes(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
						(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
						(byte) 0xFF, (byte) 0xFF, (byte) 0xFF }, 22))).build());
		assertEquals(subs.build(), parser.parseSubobject(ip6PrefixBytes));
		assertArrayEquals(ip6PrefixBytes, parser.serializeSubobject(subs.build()));
	}

	@Test
	public void testRROUnnumberedSubobject() throws PCEPDeserializerException {
		final RROUnnumberedInterfaceSubobjectParser parser = new RROUnnumberedInterfaceSubobjectParser();
		final SubobjectsBuilder subs = new SubobjectsBuilder();
		subs.setProtectionAvailable(false);
		subs.setProtectionInUse(true);
		subs.setSubobjectType(new UnnumberedBuilder().setRouterId(0x12345000L).setInterfaceId(0xffffffffL).build());
		assertEquals(subs.build(), parser.parseSubobject(unnumberedBytes));
		assertArrayEquals(unnumberedBytes, parser.serializeSubobject(subs.build()));
	}

	@Test
	public void testRROPathKey32Subobject() throws PCEPDeserializerException {
		final RROPathKeySubobjectParser parser = new RROPathKeySubobjectParser();
		final SubobjectsBuilder subs = new SubobjectsBuilder();
		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.subobjects.subobject.type.path.key.PathKeyBuilder pBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.subobjects.subobject.type.path.key.PathKeyBuilder();
		pBuilder.setPceId(new PceId(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x50, (byte) 0x00 }));
		pBuilder.setPathKey(new PathKey(4660));
		subs.setSubobjectType(new PathKeyBuilder().setPathKey(pBuilder.build()).build());
		assertEquals(subs.build(), parser.parseSubobject(pathKey32Bytes));
		assertArrayEquals(pathKey32Bytes, parser.serializeSubobject(subs.build()));
	}

	@Test
	public void testRROPathKey128Subobject() throws PCEPDeserializerException {
		final RROPathKeySubobjectParser parser = new RROPathKeySubobjectParser();
		final SubobjectsBuilder subs = new SubobjectsBuilder();
		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.subobjects.subobject.type.path.key.PathKeyBuilder pBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.subobjects.subobject.type.path.key.PathKeyBuilder();
		pBuilder.setPceId(new PceId(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE,
				(byte) 0x12, (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 }));
		pBuilder.setPathKey(new PathKey(4660));
		subs.setSubobjectType(new PathKeyBuilder().setPathKey(pBuilder.build()).build());
		assertEquals(subs.build(), parser.parseSubobject(pathKey128Bytes));
		assertArrayEquals(pathKey128Bytes, parser.serializeSubobject(subs.build()));
	}

	@Test
	public void testRROLabelSubobject() throws Exception {
		final RROLabelSubobjectParser parser = new RROLabelSubobjectParser(ServiceLoaderPCEPExtensionProviderContext.create().getLabelHandlerRegistry());
		final SubobjectsBuilder subs = new SubobjectsBuilder();
		subs.setSubobjectType(new LabelBuilder().setUniDirectional(true).setGlobal(true).setLabelType(
				new GeneralizedLabelBuilder().setGeneralizedLabel(new byte[] { (byte) 0x12, (byte) 0x00, (byte) 0x25, (byte) 0xFF }).build()).build());
		assertEquals(subs.build(), parser.parseSubobject(labelBytes));
		assertArrayEquals(labelBytes, parser.serializeSubobject(subs.build()));
	}
}
