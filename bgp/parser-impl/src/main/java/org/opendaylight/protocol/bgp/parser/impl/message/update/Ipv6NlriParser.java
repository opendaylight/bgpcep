/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.destination.destination.type.DestinationIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.destination.destination.type.DestinationIpv6Builder;

final class Ipv6NlriParser extends IpNlriParser {
	@Override
	protected DestinationIpv6 parseNlri(final byte[] nlri) {
		return new DestinationIpv6Builder().setIpv6Prefixes(Ipv6Util.prefixListForBytes(nlri)).build();
	}
}