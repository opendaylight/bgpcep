/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;

/**
 * @see <a href="http://www.cisco.com/c/en/us/support/docs/ip/border-gateway-protocol-bgp/13753-25.html">BGP Best Path
 *      Selection</a>
 */
public class BestPathSelectionTest {

	private final BGPObjectComparator comparator = new BGPObjectComparator();

	private PathAttributes attr1;
	private PathAttributes attr2;
	private PathAttributes attr3;
	private PathAttributes attr4;
	private PathAttributes attr5;
	private PathAttributes attr6;

	@Before
	public void setUp() {
		final PathAttributesBuilder builder = new PathAttributesBuilder();
		builder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Incomplete).build());
		builder.setLocalPref(new LocalPrefBuilder().setPref(100L).build());
		this.attr1 = builder.build();
		builder.setLocalPref(new LocalPrefBuilder().setPref(230L).build());
		this.attr2 = builder.build();
		builder.setOrigin(new OriginBuilder().setValue(BgpOrigin.Egp).build());
		builder.setMultiExitDisc(new MultiExitDiscBuilder().setMed(15L).build());
		this.attr3 = builder.build();
		builder.setMultiExitDisc(new MultiExitDiscBuilder().setMed(12L).build());
		this.attr4 = builder.build();

	}

	@Test
	public void testCompare() {
		assertTrue(this.comparator.compare(this.attr1, this.attr2) < 0);
		assertTrue(this.comparator.compare(this.attr2, this.attr1) > 0);

		assertTrue(this.comparator.compare(this.attr2, this.attr3) < 0);
		assertTrue(this.comparator.compare(this.attr3, this.attr2) > 0);

		assertTrue(this.comparator.compare(this.attr3, this.attr4) < 0);
		assertTrue(this.comparator.compare(this.attr4, this.attr3) > 0);
	}
}
