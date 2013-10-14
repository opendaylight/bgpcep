/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;

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
}
