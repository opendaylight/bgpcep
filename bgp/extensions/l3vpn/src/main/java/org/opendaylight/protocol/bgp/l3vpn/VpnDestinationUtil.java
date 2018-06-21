/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.l3vpn;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.protocol.bgp.labeled.unicast.LUNlriParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.labeled.unicast.LabelStack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev171207.l3vpn.ip.destination.type.VpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev171207.l3vpn.ip.destination.type.VpnDestinationBuilder;

final class VpnDestinationUtil {
    private VpnDestinationUtil() {
        throw new UnsupportedOperationException();
    }

    static List<VpnDestination> parseNlri(final ByteBuf nlri, final Class<? extends AddressFamily> afi) {
        if (!nlri.isReadable()) {
            return null;
        }
        final List<VpnDestination> dests = new ArrayList<>();

        while (nlri.isReadable()) {
            final VpnDestinationBuilder builder = new VpnDestinationBuilder();
            final short length = nlri.readUnsignedByte();
            final List<LabelStack> labels = LUNlriParser.parseLabel(nlri);
            builder.setLabelStack(labels);
            final int labelNum = labels != null ? labels.size() : 1;
            final int prefixLen = length - (LUNlriParser.LABEL_LENGTH * Byte.SIZE * labelNum)
                    - (RouteDistinguisherUtil.RD_LENGTH * Byte.SIZE);
            builder.setRouteDistinguisher(RouteDistinguisherUtil.parseRouteDistinguisher(nlri));
            Preconditions.checkState(prefixLen > 0, "A valid VPN IP prefix is required.");
            builder.setPrefix(LUNlriParser.parseIpPrefix(nlri, prefixLen, afi));
            dests.add(builder.build());
        }
        return dests;
    }
}
