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
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv6.prefixes.destination.ipv6.Ipv6Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class AdvertizedRoutesSerializer implements NlriSerializer {

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a PathAttribute object.");
        final Attributes1 pathAttributes1 = ((Attributes) attribute).getAugmentation(Attributes1.class);
        if (pathAttributes1 == null) {
            return;
        }
        final MpReachNlri mpReachNlri = pathAttributes1.getMpReachNlri();
        if (mpReachNlri == null) {
            return;
        }
        final AdvertizedRoutes routes = mpReachNlri.getAdvertizedRoutes();
        if (routes.getDestinationType() instanceof DestinationIpv4Case) {
            final DestinationIpv4Case destinationIpv4Case = (DestinationIpv4Case) routes.getDestinationType();
            for (final Ipv4Prefixes ipv4Prefix : destinationIpv4Case.getDestinationIpv4().getIpv4Prefixes()) {
                PathIdUtil.writePathId(ipv4Prefix.getPathId(), byteAggregator);
                ByteBufWriteUtil.writeMinimalPrefix(ipv4Prefix.getPrefix(), byteAggregator);
            }
        } else if (routes.getDestinationType() instanceof DestinationIpv6Case) {
            final DestinationIpv6Case destinationIpv6Case = (DestinationIpv6Case) routes.getDestinationType();
            for (final Ipv6Prefixes ipv6Prefix : destinationIpv6Case.getDestinationIpv6().getIpv6Prefixes()) {
                PathIdUtil.writePathId(ipv6Prefix.getPathId(), byteAggregator);
                ByteBufWriteUtil.writeMinimalPrefix(ipv6Prefix.getPrefix(), byteAggregator);
            }
        }
    }
}
