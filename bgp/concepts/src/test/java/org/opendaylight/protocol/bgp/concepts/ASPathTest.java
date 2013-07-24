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
import org.opendaylight.protocol.bgp.concepts.ASPath;

import org.opendaylight.protocol.concepts.ASNumber;

public class ASPathTest {

	private ASNumber asn1, asn2, asn3, asn4, asn5, asn6, asn7, asn8;

	private List<ASNumber> visibleAsPath;
	private Set<ASNumber> aggregatedAsPath;

	@Before
	public void init() {
		asn1 = new ASNumber(100, 200);
		asn2 = new ASNumber(65534);
		asn3 = new ASNumber(0, 200);
		asn4 = new ASNumber(100, 199);
		asn5 = new ASNumber(99, 199);
		asn6 = new ASNumber(64538);
		asn7 = new ASNumber(0, 200);
		asn8 = new ASNumber(100, 0);

		visibleAsPath = new ArrayList<ASNumber>();
		aggregatedAsPath = new HashSet<ASNumber>();

		visibleAsPath.add(asn1);
		visibleAsPath.add(asn2);
		visibleAsPath.add(asn3);
		visibleAsPath.add(asn4);

		aggregatedAsPath.add(asn5);
		aggregatedAsPath.add(asn6);
		aggregatedAsPath.add(asn7);
		aggregatedAsPath.add(asn8);
	}

	@Test
	public void testGetXXXPath() {
		ASPath asp1 = ASPath.EMPTY;
		assertEquals(0, asp1.getVisibleAsPath().size());
		assertEquals(0, asp1.getAggregatedAsPath().size());

		ASPath asp2 = new ASPath(visibleAsPath);
		assertEquals(4, asp2.getVisibleAsPath().size());
		assertEquals(0, asp2.getAggregatedAsPath().size());

		ASPath asp3 = new ASPath(visibleAsPath, aggregatedAsPath);
		assertEquals(4, asp3.getVisibleAsPath().size());
		assertEquals(4, asp3.getAggregatedAsPath().size());
	}

	@Test
	public void testEqualsHashCode() {
		ASPath asp1 = ASPath.EMPTY;
		ASPath asp2 = asp1;
		assertEquals(asp1, asp2);
		assertEquals(asp1.hashCode(), asp2.hashCode());
		assertNotNull(asp1);
		assertThat(asp1, not(new Object()));

		ASPath asp3 = new ASPath(visibleAsPath, aggregatedAsPath);
		assertThat(asp1, not(equalTo(asp3)));
		assertThat(asp1.hashCode(), not(equalTo(asp3.hashCode())));

		ASPath asp4 = new ASPath(new ArrayList<ASNumber>(), aggregatedAsPath);
		assertThat(asp3, not(equalTo(asp4)));
	}

	@Test
	public void testToString() {
		ASPath asp = new ASPath(visibleAsPath, aggregatedAsPath);
		assertNotNull(asp.toString());
	}

}
