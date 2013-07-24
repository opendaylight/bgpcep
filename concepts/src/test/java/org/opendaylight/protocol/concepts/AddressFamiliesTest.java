/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

public class AddressFamiliesTest {
	@Test
	public void testParseIpv4Prefix() throws UnknownHostException {
		final byte[] pref = new byte[] { (byte) 172, 17, 2 };
		final Prefix<?> after = IPv4.FAMILY.prefixForBytes(pref, 24);
		final Prefix<IPv4Address> expected = new IPv4Prefix(new IPv4Address(InetAddress.getByName("172.17.2.0")), 24);
		assertEquals(expected, after);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseIpv4Prefix2(){
		IPv4.FAMILY.prefixForBytes(new byte[5], 20);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseIpv4Prefix3(){
		IPv4.FAMILY.prefixForBytes(new byte[] { (byte) 172, 17, 2 }, 33);
	}

	@Test
	public void testParseIpv6Prefix() throws UnknownHostException {
		final byte[] pref = new byte[] { (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x02 };
		final Prefix<?> after =IPv6.FAMILY.prefixForBytes(pref, 64);
		final Prefix<IPv6Address> expected = new IPv6Prefix(new IPv6Address(InetAddress.getByName("2001:db8:1:2::")), 64);
		assertEquals(expected, after);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseIpv6Prefix2(){
		IPv4.FAMILY.prefixForBytes(new byte[17], 30);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseIpv6Prefix3(){
		IPv4.FAMILY.prefixForBytes(new byte[] { (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x02 }, 129);
	}

	@Test
	public void testParseIpv6PrefixList(){
		assertTrue(IPv6.FAMILY.prefixListForBytes(new byte[0]).isEmpty());
	}

	@Test
	public void testParseIpv6PrefixList2(){
		assertEquals(2, IPv6.FAMILY.prefixListForBytes(new byte[] { (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8, (byte) 0x00, (byte) 0x01, (byte) 0x00, }).size());
	}

	@Test
	public void testParseIpv4PrefixList(){
		assertTrue(IPv4.FAMILY.prefixListForBytes(new byte[0]).isEmpty());
	}

	@Test
	public void testParseIpv4PrefixList2(){
		assertFalse(IPv4.FAMILY.prefixListForBytes(new byte[] { (byte) 15, 17, 2 }).isEmpty());
	}
}

