/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.destination.ipv6._case.DestinationIpv6Builder;

public final class Ipv6NlriParser extends IpNlriParser {
    @Override
    protected DestinationIpv6Case parseNlri(final ByteBuf nlri) {
        return new DestinationIpv6CaseBuilder().setDestinationIpv6(
                new DestinationIpv6Builder().setIpv6Prefixes(Ipv6Util.prefixListForBytes(ByteArray.readAllBytes(nlri))).build()).build();
    }
}