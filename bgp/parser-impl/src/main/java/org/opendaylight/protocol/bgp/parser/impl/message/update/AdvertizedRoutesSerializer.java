/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class AdvertizedRoutesSerializer implements NlriSerializer {
    @Override
    public void serializeAttribute(DataObject attribute, ByteBuf byteAggregator) {
        PathAttributes pathAttributes = (PathAttributes) attribute;
        if (pathAttributes.getAugmentation(PathAttributes1.class) == null) {
            return;
        }
        if (pathAttributes.getAugmentation(PathAttributes1.class).getMpReachNlri() == null){
            return;
        }
        PathAttributes1 pathAttributes1 = pathAttributes.getAugmentation(PathAttributes1.class);
        MpReachNlri mpReachNlri = pathAttributes1.getMpReachNlri();

        if (mpReachNlri.getAdvertizedRoutes().getDestinationType() instanceof DestinationIpv4Case) {
            DestinationIpv4Case destinationIpv4Case = (DestinationIpv4Case) mpReachNlri.getAdvertizedRoutes().getDestinationType();
            for (Ipv4Prefix ipv4Prefix : destinationIpv4Case.getDestinationIpv4().getIpv4Prefixes()) {
                byteAggregator.writeByte(Ipv4Util.getPrefixLength(ipv4Prefix.getValue()));
                byteAggregator.writeBytes(Ipv4Util.bytesForPrefixByPrefixLength(ipv4Prefix));
            }
        } else if (mpReachNlri.getAdvertizedRoutes().getDestinationType() instanceof DestinationIpv6Case) {
            DestinationIpv6Case destinationIpv6Case = (DestinationIpv6Case) mpReachNlri.getAdvertizedRoutes().getDestinationType();
            for (Ipv6Prefix ipv6Prefix : destinationIpv6Case.getDestinationIpv6().getIpv6Prefixes()) {
                byteAggregator.writeByte(Ipv4Util.getPrefixLength(ipv6Prefix.getValue()));
                byteAggregator.writeBytes(Ipv6Util.bytesForPrefixByPrefixLength(ipv6Prefix));
            }
        }
    }
}
