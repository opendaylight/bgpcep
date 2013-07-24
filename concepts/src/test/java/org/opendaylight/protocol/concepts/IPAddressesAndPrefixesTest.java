/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv6;
import org.opendaylight.protocol.concepts.IPv6Address;

public class IPAddressesAndPrefixesTest {

	@Test
	public void test1(){
		assertTrue(IPAddresses.parseNetworkAddress("123.123.123.123") instanceof IPv4Address);
		assertTrue(IPAddresses.parseNetworkAddress("2001::1") instanceof IPv6Address);
	}

	@Test(expected = IllegalArgumentException.class)
	public void test2(){
		IPAddresses.parseNetworkAddress("256.125.126.256");
	}

	@Test
	public void test3() {
		assertTrue("123.123.123.123".equals(IPv4.FAMILY.addressForString("123.123.123.123").toString()));
		assertTrue("2001:0:0:0:0:0:0:1".equals(IPv6.FAMILY.addressForString("2001::1").toString()));
	}

	@Test
	public void test4() throws UnknownHostException{
		assertTrue(IPAddresses.createNetworkAddress(InetAddress.getByName("123.123.123.123")) instanceof IPv4Address);
		assertTrue(IPAddresses.createNetworkAddress(InetAddress.getByName("2001::1")) instanceof IPv6Address);
	}

	@Test
	public void test5() {
		assertTrue("123.123.123.0/24".equals(IPv4.FAMILY.addressForString("123.123.123.123").asPrefix(24).toString()));
		assertTrue("123.123.123.0/24".equals(IPv4.FAMILY.prefixForString("123.123.123.123/24").toString()));
		assertTrue("2001:0:0:0:0:0:0:0/120".equals(IPv6.FAMILY.addressForString("2001::1").asPrefix(120).toString()));
		assertTrue("2001:0:0:0:0:0:0:0/120".equals(IPv6.FAMILY.prefixForString("2001::1/120").toString()));
	}
}
