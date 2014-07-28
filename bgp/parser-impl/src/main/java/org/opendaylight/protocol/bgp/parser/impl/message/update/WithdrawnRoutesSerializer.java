/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.unreach.nlri.WithdrawnRoutes;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class WithdrawnRoutesSerializer implements NlriSerializer {

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof PathAttributes, "Attribute parameter is not a PathAttribute object.");
        final PathAttributes2 pathAttributes2 = ((PathAttributes) attribute).getAugmentation(PathAttributes2.class);
        if (pathAttributes2 == null) {
            return;
        }
        final MpUnreachNlri mpUnreachNlri = pathAttributes2.getMpUnreachNlri();
        if (mpUnreachNlri == null) {
            return;
        }
        final WithdrawnRoutes routes = mpUnreachNlri.getWithdrawnRoutes();
        if (routes != null) {
            if (routes.getDestinationType() instanceof DestinationIpv4Case) {
                final DestinationIpv4Case destinationIpv4Case = (DestinationIpv4Case)routes.getDestinationType();
                if (destinationIpv4Case.getDestinationIpv4().getIpv4Prefixes() != null) {
                    for (final Ipv4Prefix ipv4Prefix : destinationIpv4Case.getDestinationIpv4().getIpv4Prefixes()) {
                        byteAggregator.writeBytes(Ipv4Util.bytesForPrefixBegin(ipv4Prefix));
                    }
                }
            } else if (routes.getDestinationType() instanceof DestinationIpv6Case) {
                final  DestinationIpv6Case destinationIpv6Case = (DestinationIpv6Case) routes.getDestinationType();
                if (destinationIpv6Case.getDestinationIpv6().getIpv6Prefixes() != null) {
                    for (final Ipv6Prefix ipv6Prefix : destinationIpv6Case.getDestinationIpv6().getIpv6Prefixes()) {
                        byteAggregator.writeBytes(Ipv6Util.bytesForPrefixBegin(ipv6Prefix));
                    }
                }
            }
        }
    }
}
