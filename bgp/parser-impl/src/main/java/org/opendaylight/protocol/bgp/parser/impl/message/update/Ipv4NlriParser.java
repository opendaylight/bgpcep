/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv4Builder;

public final class Ipv4NlriParser extends IpNlriParser {
	@Override
	protected DestinationIpv4 parseNlri(final byte[] nlri) {
		return new DestinationIpv4Builder().setIpv4Prefixes(Ipv4Util.prefixListForBytes(nlri)).build();
	}
}