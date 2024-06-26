/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.impl.nlri;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.bgp.concepts.IpAddressUtil;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.c.multicast.grouping.CMulticast;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.c.multicast.grouping.CMulticastBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Common CMulticast Handler.
 *
 * @author Claudio D. Gasparini
 */
final class CMulticastUtil {
    private CMulticastUtil() {
        // Hidden on purpose
    }

    static CMulticast parseCMulticastGrouping(final ByteBuf buffer) {
        return new CMulticastBuilder()
            .setRouteDistinguisher(RouteDistinguisherUtil.parseRouteDistinguisher(buffer))
            .setSourceAs(new AsNumber(ByteBufUtils.readUint32(buffer)))
            .setMulticastSource(IpAddressUtil.addressForByteBuf(buffer))
            .setMulticastGroup(MulticastGroupOpaqueUtil.multicastGroupForByteBuf(buffer))
            .build();
    }

    static ByteBuf serializeCMulticast(final CMulticast route) {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        RouteDistinguisherUtil.serializeRouteDistinquisher(route.getRouteDistinguisher(), nlriByteBuf);
        nlriByteBuf.writeInt(route.getSourceAs().getValue().intValue());
        IpAddressUtil.writeBytesFor(route.getMulticastSource(), nlriByteBuf);
        MulticastGroupOpaqueUtil.bytesForMulticastGroup(route.getMulticastGroup(), nlriByteBuf);
        return nlriByteBuf;
    }
}
