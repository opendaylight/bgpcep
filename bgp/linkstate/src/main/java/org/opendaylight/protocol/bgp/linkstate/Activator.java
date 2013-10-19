/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;

/**
 *
 */
public final class Activator implements BGPExtensionProviderActivator {
	@Override
	public void start(final BGPExtensionProviderContext context) throws Exception {
		context.registerAddressFamily(LinkstateAddressFamily.class, 16388);
		context.registerSubsequentAddressFamily(LinkstateSubsequentAddressFamily.class, 71);

		context.registerNlriParser(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class,
				new LinkstateNlriParser(false));
		context.registerNlriParser(LinkstateAddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class,
				new LinkstateNlriParser(true));

		context.registerAttributeParser(LinkstateAttributeParser.TYPE, new LinkstateAttributeParser());
	}

	@Override
	public void stop() throws Exception {
		// TODO Auto-generated method stub
	}
}
