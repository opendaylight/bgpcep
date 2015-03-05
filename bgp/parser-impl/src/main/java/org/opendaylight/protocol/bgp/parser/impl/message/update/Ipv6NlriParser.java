/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.ipv6.prefixes.DestinationIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.ipv6.prefixes.destination.ipv6.Ipv6Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.ipv6.prefixes.destination.ipv6.Ipv6PrefixesBuilder;

public final class Ipv6NlriParser extends IpNlriParser {
    @Override
    protected DestinationIpv6Case parseNlri(final ByteBuf nlri) {
        final List<Ipv6Prefix> prefs = Ipv6Util.prefixListForBytes(ByteArray.readAllBytes(nlri));
        final List<Ipv6Prefixes> prefixes = new ArrayList<>();
        for (final Ipv6Prefix p : prefs) {
            prefixes.add(new Ipv6PrefixesBuilder().setPrefix(p).build());
        }
        return new DestinationIpv6CaseBuilder().setDestinationIpv6(
                new DestinationIpv6Builder().setIpv6Prefixes(prefixes).build()).build();
    }
}