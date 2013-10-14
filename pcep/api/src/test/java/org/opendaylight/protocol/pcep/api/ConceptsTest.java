/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.api;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.pcep.concepts.AbstractExtendedTunnelIdentifier;
import org.opendaylight.protocol.pcep.concepts.AggregateBandwidthConsumptionMetric;
import org.opendaylight.protocol.pcep.concepts.CumulativeIGPCostMetric;
import org.opendaylight.protocol.pcep.concepts.CumulativeTECostMetric;
import org.opendaylight.protocol.pcep.concepts.IPv4ExtendedTunnelIdentifier;
import org.opendaylight.protocol.pcep.concepts.IPv6ExtendedTunnelIdentifier;
import org.opendaylight.protocol.pcep.concepts.LSPIdentifier;
import org.opendaylight.protocol.pcep.concepts.LSPSymbolicName;
import org.opendaylight.protocol.pcep.concepts.MostLoadedLinkLoadMetric;
import org.opendaylight.protocol.pcep.concepts.TunnelIdentifier;
import org.opendaylight.protocol.pcep.concepts.UnnumberedInterfaceIdentifier;

public class ConceptsTest {

	@Test
	public void testConcepts() throws UnknownHostException {
		final AbstractExtendedTunnelIdentifier<IPv4Address> t1 = new AbstractExtendedTunnelIdentifier<IPv4Address>(new IPv4Address(
				InetAddress.getByName("127.0.0.1"))) {
			private static final long serialVersionUID = 445350555352830607L;
		};

		final AbstractExtendedTunnelIdentifier<IPv4Address> t2 = new AbstractExtendedTunnelIdentifier<IPv4Address>(new IPv4Address(
				InetAddress.getByName("127.0.0.2"))) {
			private static final long serialVersionUID = 572633522583009640L;
		};

		final AbstractExtendedTunnelIdentifier<IPv4Address> t3 = new AbstractExtendedTunnelIdentifier<IPv4Address>(new IPv4Address(
				InetAddress.getByName("127.0.0.1"))) {
			private static final long serialVersionUID = 445350555352830607L;
		};

		assertNotSame(t1, t2);
		assertEquals(-1, t1.compareTo(t2));
		assertEquals(t1.hashCode(), t3.hashCode());
		assertEquals(t1.toString(), t3.toString());

		final IPv4ExtendedTunnelIdentifier v4 = new IPv4ExtendedTunnelIdentifier(new IPv4Address(InetAddress.getByName("127.0.0.1")));
		final IPv6ExtendedTunnelIdentifier v6 = new IPv6ExtendedTunnelIdentifier(new IPv6Address(InetAddress.getByName("2001:db8:85a3::8a2e:370:7333")));
		assertTrue(v4 instanceof AbstractExtendedTunnelIdentifier);
		assertTrue(v6 instanceof AbstractExtendedTunnelIdentifier);

		final LSPIdentifier id1 = new LSPIdentifier(new byte[] { 1, 2 });
		final LSPIdentifier id2 = new LSPIdentifier(new byte[] { 1, 3 });
		final LSPIdentifier id3 = new LSPIdentifier(new byte[] { 1, 3 });

		assertNotSame(id1, id2);
		assertNotSame(id1.getLspId(), id2.getLspId());
		assertEquals(id3.toString(), id2.toString());

		final LSPSymbolicName n1 = new LSPSymbolicName(new byte[] { 5 });
		final LSPSymbolicName n2 = new LSPSymbolicName(new byte[] { 6, 3 });
		final LSPSymbolicName n3 = new LSPSymbolicName(new byte[] { 5 });
		assertNotSame(n1.getSymbolicName(), n2.getSymbolicName());
		assertEquals(n1, n3);
		assertEquals(n1.toString(), n3.toString());

		final TunnelIdentifier ti1 = new TunnelIdentifier(new byte[] { 2, 4 });
		final TunnelIdentifier ti2 = new TunnelIdentifier(new byte[] { 2, 4 });
		assertArrayEquals(ti1.getBytes(), ti2.getBytes());
		assertEquals(ti1, ti2);
		assertEquals(ti1.toString(), ti2.toString());

		final UnnumberedInterfaceIdentifier u1 = new UnnumberedInterfaceIdentifier(3000);
		final UnnumberedInterfaceIdentifier u2 = new UnnumberedInterfaceIdentifier(4000);
		final UnnumberedInterfaceIdentifier u3 = new UnnumberedInterfaceIdentifier(3000);

		assertEquals(-1, u1.compareTo(u2));
		assertEquals(u1, u3);
		assertEquals(u1.hashCode(), u3.hashCode());
		assertEquals(u1.getInterfaceId(), u3.getInterfaceId());
		assertEquals(u1.toString(), u3.toString());

		final CumulativeIGPCostMetric cigp1 = new CumulativeIGPCostMetric(3000);
		final CumulativeIGPCostMetric cigp2 = new CumulativeIGPCostMetric(4000);
		final CumulativeIGPCostMetric cigp3 = new CumulativeIGPCostMetric(3000);
		try {
			new CumulativeIGPCostMetric(-1);
			fail("Expected exception but no thrown.");
		} catch (final IllegalArgumentException e) {
		}

		assertEquals(-1, cigp1.compareTo(cigp2));
		assertEquals(cigp1, cigp3);
		assertEquals(cigp1.hashCode(), cigp3.hashCode());
		assertEquals(cigp1.getValue(), cigp3.getValue());
		assertEquals(cigp1.toString(), cigp3.toString());

		final CumulativeTECostMetric cte1 = new CumulativeTECostMetric(3000);
		final CumulativeTECostMetric cte2 = new CumulativeTECostMetric(4000);
		final CumulativeTECostMetric cte3 = new CumulativeTECostMetric(3000);

		assertEquals(-1, cte1.compareTo(cte2));
		assertEquals(cte1, cte3);
		assertEquals(cte1.hashCode(), cte3.hashCode());
		assertEquals(cte1.getValue(), cte3.getValue());
		assertEquals(cte1.toString(), cte3.toString());
		try {
			new CumulativeTECostMetric(-1);
			fail("Expected exception but no thrown.");
		} catch (final IllegalArgumentException e) {
		}

		final AggregateBandwidthConsumptionMetric agg1 = new AggregateBandwidthConsumptionMetric(3000);
		final AggregateBandwidthConsumptionMetric agg2 = new AggregateBandwidthConsumptionMetric(4000);
		final AggregateBandwidthConsumptionMetric agg3 = new AggregateBandwidthConsumptionMetric(3000);

		assertEquals(-1, agg1.compareTo(agg2));
		assertEquals(agg1, agg3);
		assertEquals(agg1.hashCode(), agg3.hashCode());
		assertEquals(agg1.getValue(), agg3.getValue());
		assertEquals(agg1.toString(), agg3.toString());
		try {
			new AggregateBandwidthConsumptionMetric(-1);
			fail("Expected exception but no thrown.");
		} catch (final IllegalArgumentException e) {
		}

		final MostLoadedLinkLoadMetric mlm1 = new MostLoadedLinkLoadMetric(3000);
		final MostLoadedLinkLoadMetric mlm2 = new MostLoadedLinkLoadMetric(4000);
		final MostLoadedLinkLoadMetric mlm3 = new MostLoadedLinkLoadMetric(3000);

		assertEquals(-1, mlm1.compareTo(mlm2));
		assertEquals(mlm1, mlm3);
		assertEquals(mlm1.hashCode(), mlm3.hashCode());
		assertEquals(mlm1.getValue(), mlm3.getValue());
		assertEquals(mlm1.toString(), mlm3.toString());
		try {
			new MostLoadedLinkLoadMetric(-1);
			fail("Expected exception but no thrown.");
		} catch (final IllegalArgumentException e) {
		}
	}
}
