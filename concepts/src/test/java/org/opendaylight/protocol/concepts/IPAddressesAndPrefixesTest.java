/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;
import java.util.List;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;

import com.google.common.collect.Lists;

public class IPAddressesAndPrefixesTest {

	@Test
	public void test3() {
		assertTrue("123.123.123.123".equals(new Ipv4Address("123.123.123.123").getValue()));
		assertTrue("2001::1".equals(new Ipv6Address("2001::1").getValue()));
	}

	@Test
	public void test4() throws UnknownHostException {
		assertTrue(new IpPrefix(new Ipv4Prefix("123.123.123.123")).getIpv4Prefix() != null);
		assertTrue(new IpPrefix(new Ipv6Prefix("2001::1")).getIpv6Prefix() != null);
	}

	@Test
	public void test5() {
		assertTrue("123.123.123.123/24".equals(new Ipv4Prefix("123.123.123.123/24").getValue()));
		assertTrue("2001::1/120".equals(new Ipv6Prefix("2001::1/120").getValue()));
	}

	@Test
	public void testPrefix4ForBytes() {
		byte[] bytes = new byte[] { 123, 122, 4, 5 };
		assertEquals(new Ipv4Prefix("123.122.4.5/8"), Ipv4Util.prefixForBytes(bytes, 8));
		assertArrayEquals(new byte[] { 102, 0, 0, 0, 8 }, Ipv4Util.bytesForPrefix(new Ipv4Prefix("102.0.0.0/8")));

		bytes = new byte[] { (byte) 255, (byte) 255, 0, 0 };
		assertEquals(new Ipv4Prefix("255.255.0.0/16"), Ipv4Util.prefixForBytes(bytes, 16));

		assertArrayEquals(new byte[] { (byte) 255, (byte) 255, 0, 0, 16 }, Ipv4Util.bytesForPrefix(new Ipv4Prefix("255.255.0.0/16")));
	}

	@Test
	public void testAddress4ForBytes() {
		final byte[] bytes = new byte[] { (byte) 123, (byte) 122, (byte) 4, (byte) 5 };
		assertEquals(new Ipv4Address("123.122.4.5"), Ipv4Util.addressForBytes(bytes));
		try {
			Ipv4Util.addressForBytes(new byte[] { 22, 44, 66, 18, 88, 33 });
			fail();
		} catch (final IllegalArgumentException e) {
			assertEquals("addr is of illegal length", e.getMessage());
		}
	}

	@Test
	public void testPrefixList4ForBytes() {
		final byte[] bytes = new byte[] { 22, (byte) 172, (byte) 168, 3, 8, 12, 32, (byte) 192, (byte) 168, 35, 100 };
		final List<Ipv4Prefix> prefs = Ipv4Util.prefixListForBytes(bytes);
		assertEquals(
				Lists.newArrayList(new Ipv4Prefix("172.168.3.0/22"), new Ipv4Prefix("12.0.0.0/8"), new Ipv4Prefix("192.168.35.100/32")),
				prefs);
	}

	@Test
	public void testPrefix6ForBytes() {
		final byte[] bytes = new byte[] { 0x20, 0x01, 0x0d, (byte) 0xb8, 0x00, 0x01, 0x00, 0x02 };
		assertEquals(new Ipv6Prefix("2001:db8:1:2::/64"), Ipv6Util.prefixForBytes(bytes, 64));
	}

	@Test
	public void testPrefixList6ForBytes() {
		final List<Ipv6Prefix> prefs = Lists.newArrayList();
		prefs.add(new Ipv6Prefix("2001:db8:1:2::/64"));
		prefs.add(new Ipv6Prefix("2001:db8:1:1::/64"));
		prefs.add(new Ipv6Prefix("2001:db8:1::/64"));

	}

	@Test
	public void testPrefixLength() {
		assertEquals(22, Ipv4Util.getPrefixLength(new IpPrefix(new Ipv4Prefix("172.168.3.0/22"))));
		assertEquals(64, Ipv4Util.getPrefixLength(new IpPrefix(new Ipv6Prefix("2001:db8:1:2::/64"))));
	}
}
