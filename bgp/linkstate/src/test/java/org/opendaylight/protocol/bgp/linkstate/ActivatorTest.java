/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateSubsequentAddressFamily;

public class ActivatorTest {

	private final BGPActivator act = new BGPActivator();

	@Test
	public void testActivator() throws Exception {
		final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();

		assertNull(context.getAddressFamilyRegistry().classForFamily(16388));
		assertNull(context.getSubsequentAddressFamilyRegistry().classForFamily(71));

		this.act.start(context);

		assertEquals(LinkstateAddressFamily.class, context.getAddressFamilyRegistry().classForFamily(16388));
		assertEquals(LinkstateSubsequentAddressFamily.class, context.getSubsequentAddressFamilyRegistry().classForFamily(71));
	}

	@After
	public void tearDown() {
		try {
			this.act.stop();
		} catch (final Exception e) {
			fail("This exception should not occurr.");
		}
	}
}
