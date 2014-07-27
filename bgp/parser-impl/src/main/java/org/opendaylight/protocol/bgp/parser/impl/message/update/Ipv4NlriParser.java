/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.destination.ipv4._case.DestinationIpv4Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Ipv4NlriParser extends IpNlriParser implements NlriSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv4NlriParser.class);

    @Override
    protected DestinationIpv4Case parseNlri(final ByteBuf nlri) {
        return new DestinationIpv4CaseBuilder().setDestinationIpv4(
                new DestinationIpv4Builder().setIpv4Prefixes(Ipv4Util.prefixListForBytes(ByteArray.readAllBytes(nlri))).build()).build();
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        if (!(attribute instanceof Nlri)) {
            LOG.warn("Attribute parameter is not a NLRI object.");
            return;
        }
        final Nlri nlri = (Nlri) attribute;
        for (final Ipv4Prefix ipv4Prefix : nlri.getNlri()) {
            byteAggregator.writeBytes(Ipv4Util.bytesForPrefix(ipv4Prefix));
        }
    }
}