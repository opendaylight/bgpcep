/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.AbstractBGPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;

public final class BGPActivator extends AbstractBGPExtensionProviderActivator {
	@Override
	protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
		final List<AutoCloseable> regs = new ArrayList<>();

		regs.add(context.registerAddressFamily(LinkstateAddressFamily.class, 16388));
		regs.add(context.registerSubsequentAddressFamily(LinkstateSubsequentAddressFamily.class, 71));

		regs.add(context.registerNlriParser(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class, new LinkstateNlriParser(false)));
		regs.add(context.registerNlriParser(LinkstateAddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class, new LinkstateNlriParser(true)));

		regs.add(context.registerAttributeParser(LinkstateAttributeParser.TYPE, new LinkstateAttributeParser()));

		return regs;
	}
}
