/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.concepts;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

public class ASPathTest {

	private AsNumber asn1, asn2, asn3, asn4, asn5, asn6, asn7, asn8;

	private List<AsNumber> visibleAsPath;
	private Set<AsNumber> aggregatedAsPath;

	@Before
	public void init() {
		this.asn1 = new AsNumber(429496729800L);
		this.asn2 = new AsNumber((long) 65534);
		this.asn3 = new AsNumber((long) 200);
		this.asn4 = new AsNumber(429496729799L);
		this.asn5 = new AsNumber(425201762503L);
		this.asn6 = new AsNumber((long) 64538);
		this.asn7 = new AsNumber((long) 200);
		this.asn8 = new AsNumber(429496729600L);

		this.visibleAsPath = new ArrayList<AsNumber>();
		this.aggregatedAsPath = new HashSet<AsNumber>();

		this.visibleAsPath.add(this.asn1);
		this.visibleAsPath.add(this.asn2);
		this.visibleAsPath.add(this.asn3);
		this.visibleAsPath.add(this.asn4);

		this.aggregatedAsPath.add(this.asn5);
		this.aggregatedAsPath.add(this.asn6);
		this.aggregatedAsPath.add(this.asn7);
		this.aggregatedAsPath.add(this.asn8);
	}

	@Test
	public void testGetXXXPath() {
		final ASPath asp1 = ASPath.EMPTY;
		assertEquals(0, asp1.getVisibleAsPath().size());
		assertEquals(0, asp1.getAggregatedAsPath().size());

		final ASPath asp2 = new ASPath(this.visibleAsPath);
		assertEquals(4, asp2.getVisibleAsPath().size());
		assertEquals(0, asp2.getAggregatedAsPath().size());

		final ASPath asp3 = new ASPath(this.visibleAsPath, this.aggregatedAsPath);
		assertEquals(4, asp3.getVisibleAsPath().size());
		assertEquals(4, asp3.getAggregatedAsPath().size());
	}

	@Test
	public void testEqualsHashCode() {
		final ASPath asp1 = ASPath.EMPTY;
		final ASPath asp2 = asp1;
		assertEquals(asp1, asp2);
		assertEquals(asp1.hashCode(), asp2.hashCode());
		assertNotNull(asp1);
		assertThat(asp1, not(new Object()));

		final ASPath asp3 = new ASPath(this.visibleAsPath, this.aggregatedAsPath);
		assertThat(asp1, not(equalTo(asp3)));
		assertThat(asp1.hashCode(), not(equalTo(asp3.hashCode())));

		final ASPath asp4 = new ASPath(new ArrayList<AsNumber>(), this.aggregatedAsPath);
		assertThat(asp3, not(equalTo(asp4)));
	}

	@Test
	public void testToString() {
		final ASPath asp = new ASPath(this.visibleAsPath, this.aggregatedAsPath);
		assertNotNull(asp.toString());
	}

}
