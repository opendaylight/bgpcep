/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.update.path.attributes.linkstate.path.attribute.link.state.attribute.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.update.path.attributes.linkstate.path.attribute.link.state.attribute.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;

public class LinkstateAttributeParserTest {

	private static final byte[] LINK_ATTR = new byte[] { (byte) 0x04, (byte) 0x04, (byte) 0x00, (byte) 0x04, (byte) 0x2a, (byte) 0x2a,
			(byte) 0x2a, (byte) 0x2a, (byte) 0x04, (byte) 0x06, (byte) 0x00, (byte) 0x04, (byte) 0x2b, (byte) 0x2b, (byte) 0x2b,
			(byte) 0x2b, (byte) 0x04, (byte) 0x40, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x04, (byte) 0x41, (byte) 0x00, (byte) 0x04, (byte) 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80, (byte) 0x04,
			(byte) 0x42, (byte) 0x00, (byte) 0x04, (byte) 0x46, (byte) 0x43, (byte) 0x50, (byte) 0x00, (byte) 0x04, (byte) 0x43,
			(byte) 0x00, (byte) 0x20, (byte) 0x46, (byte) 0x43, (byte) 0x50, (byte) 0x00, (byte) 0x46, (byte) 0x43, (byte) 0x50,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x44,
			(byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x47, (byte) 0x00, (byte) 0x03,
			(byte) 0x00, (byte) 0x00, (byte) 0x0a };

	private static final byte[] NODE_ATTR = new byte[] { (byte) 0x04, (byte) 0x02, (byte) 0x00, (byte) 0x05, (byte) 0x31, (byte) 0x32,
			(byte) 0x4b, (byte) 0x2d, (byte) 0x32, (byte) 0x04, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x72, (byte) 0x04,
			(byte) 0x04, (byte) 0x00, (byte) 0x04, (byte) 0x29, (byte) 0x29, (byte) 0x29, (byte) 0x29 };

	private final LinkstateAttributeParser parser = new LinkstateAttributeParser();

	@Test
	public void testPositiveLinks() {
		final PathAttributesBuilder builder = new PathAttributesBuilder();
		try {
			this.parser.parseAttribute(LINK_ATTR, builder);
		} catch (final BGPParsingException e) {
			fail("No exception should occur.");
		}
		final PathAttributes1 attrs = builder.getAugmentation(PathAttributes1.class);
		final LinkAttributes ls = (LinkAttributes) attrs.getLinkstatePathAttribute().getLinkStateAttribute();
		assertNotNull(ls);

		assertEquals("42.42.42.42", ls.getLocalIpv4RouterId().getValue());
		assertEquals(new Long(10), ls.getMetric().getValue());
		assertEquals(new Long(0), ls.getAdminGroup().getValue());
		assertEquals("43.43.43.43", ls.getRemoteIpv4RouterId().getValue());
		assertArrayEquals(new byte[] { 0, 0, 0, 0, (byte) 0x49, (byte) 0x98, (byte) 0x96, (byte) 0x80 },
				ls.getMaxLinkBandwidth().getValue());
		assertArrayEquals(new byte[] { 0, 0, 0, 0, (byte) 0x46, (byte) 0x43, (byte) 0x50, (byte) 0x00 },
				ls.getMaxReservableBandwidth().getValue());
		assertNotNull(ls.getUnreservedBandwidth());
		assertEquals(8, ls.getUnreservedBandwidth().size());
	}

	@Test
	public void testPositiveNodes() {
		final PathAttributesBuilder builder = new PathAttributesBuilder();
		try {
			this.parser.parseAttribute(NODE_ATTR, builder);
		} catch (final BGPParsingException e) {
			fail("No exception should occur.");
		}
		final PathAttributes1 attrs = builder.getAugmentation(PathAttributes1.class);
		final NodeAttributes ls = (NodeAttributes) attrs.getLinkstatePathAttribute().getLinkStateAttribute();
		assertNotNull(ls);

		assertEquals("12K-2", ls.getDynamicHostname());
		assertEquals(1, ls.getIsisAreaId().size());
		assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 114 }, ls.getIsisAreaId().get(0).getValue());
		assertEquals("41.41.41.41", ls.getIpv4RouterId().getValue());
	}
}
