/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.nps.concepts.rev130930.IsoSystemIdentifier;

public class LinkAnchorTest {

	@Test
	public void test() {
		try {
			new LinkAnchor(null, null);
		} catch (final NullPointerException e) {
			assertEquals("Can not create link anchor with null node identifier.", e.getMessage());
		}

		final LinkAnchor la1 = new LinkAnchor(NodeIdentifierFactory.localIdentifier(new OSPFRouterIdentifier(new byte[] { 1, 2, 3, 4 })), IPv6InterfaceIdentifier.forString("2001:db8:85a3::8a2e:370:7334"));

		final LinkAnchor la2 = new LinkAnchor(NodeIdentifierFactory.localIdentifier(new ISISRouterIdentifier(new IsoSystemIdentifier(new byte[] {
				1, 2, 3, 4, 5, 6 }))), null);

		final LinkAnchor la3 = new LinkAnchor(NodeIdentifierFactory.localIdentifier(new OSPFRouterIdentifier(new byte[] { 1, 2, 3, 4 })), IPv6InterfaceIdentifier.forString("2001:db8:85a3::8a2e:370:7334"));

		assertNotSame(la1, la2);
		assertNotSame(la1.hashCode(), la2.hashCode());

		assertEquals(la1, la3);
		assertEquals(la1.toString(), la3.toString());
		assertEquals(la1.getNodeIdentifier(), la3.getNodeIdentifier());

		assertNull(la2.getInterfaceIdentifier());
	}

	@Test
	public void testEnums() throws UnknownHostException {
		assertEquals(MPLSProtocol.LDP.toString(), "LDP");
		assertEquals(LinkProtectionType.ENHANCED.toString(), "ENHANCED");

		final InterfaceIdentifier ii1 = new IPv6InterfaceIdentifier(new IPv6Address(InetAddress.getByName("2001:db8:85a3::8a2e:370:7334")));
		final InterfaceIdentifier ii2 = new IPv6InterfaceIdentifier(new IPv6Address(InetAddress.getByName("2001:db8:85a3::8a2e:370:7335")));

		assertEquals(-1, ((IPv6InterfaceIdentifier) ii1).compareTo((IPv6InterfaceIdentifier) ii2));
		assertEquals(0, ((IPv6InterfaceIdentifier) ii1).compareTo((IPv6InterfaceIdentifier) ii1));

		assertNotSame(ii1, ii2);

		assertNotNull(((IPv6InterfaceIdentifier) ii1).getValue());

		try {
			new IPv4InterfaceIdentifier(null);
		} catch (final NullPointerException e) {
			assertEquals("Unsupported identifier value", e.getMessage());
		}

		try {
			new IPv6InterfaceIdentifier(null);
		} catch (final NullPointerException e) {
			assertEquals("Unsupported identifier value", e.getMessage());
		}

		final InterfaceIdentifier ip1 = new IPv4InterfaceIdentifier(new IPv4Address(InetAddress.getByName("127.0.0.1")));
		final InterfaceIdentifier ip2 = new IPv4InterfaceIdentifier(new IPv4Address(InetAddress.getByName("127.0.0.2")));

		assertEquals(-1, ((IPv4InterfaceIdentifier) ip1).compareTo((IPv4InterfaceIdentifier) ip2));
		assertEquals(0, ((IPv4InterfaceIdentifier) ip1).compareTo((IPv4InterfaceIdentifier) ip1));

		assertNotSame(ip1, ip2);

		assertNotNull(((IPv4InterfaceIdentifier) ip1).getValue());
	}
}
