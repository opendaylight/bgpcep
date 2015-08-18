/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.impl.te;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.rsvp.parser.spi.subobjects.AbstractRSVPObjectParser;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.Ipv6DetourObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.Ipv6DetourObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.ipv6.detour.object.AvoidNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.ipv6.detour.object.AvoidNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.ipv6.detour.object.PlrId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.ipv6.detour.object.PlrIdBuilder;

public final class DetourObjectIpv6Parser extends AbstractRSVPObjectParser {

    public static final short CLASS_NUM = 63;
    public static final short CTYPE = 8;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) {
        final ByteBuf plrId = byteBuf.readSlice(byteBuf.capacity() / 2);
        final Ipv6DetourObjectBuilder ipv6Case = new Ipv6DetourObjectBuilder();

        final List<PlrId> plrIdList = new ArrayList<>();

        while (plrId.isReadable()) {
            final PlrIdBuilder plr = new PlrIdBuilder();
            plr.setPlrId(Ipv6Util.addressForByteBuf(plrId));
            plrIdList.add(plr.build());
        }

        final List<AvoidNode> avoidNodeList = new ArrayList<>();
        while (byteBuf.isReadable()) {
            final AvoidNodeBuilder plr = new AvoidNodeBuilder();
            plr.setAvoidNode(Ipv6Util.addressForByteBuf(byteBuf));
            avoidNodeList.add(plr.build());
        }

        return ipv6Case.setPlrId(plrIdList).setAvoidNode(avoidNodeList).build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(teLspObject instanceof Ipv6DetourObject, "DetourObject is mandatory.");
        final Ipv6DetourObject detourObject = (Ipv6DetourObject) teLspObject;

        final List<PlrId> pList = detourObject.getPlrId();
        final List<AvoidNode> aList = detourObject.getAvoidNode();
        serializeAttributeHeader((pList.size() * Ipv6Util.IPV6_LENGTH) + (aList.size() * Ipv6Util.IPV6_LENGTH), CLASS_NUM,
            CTYPE, byteAggregator);

        for (PlrId plrId : pList) {
            byteAggregator.writeBytes(Ipv6Util.byteBufForAddress(plrId.getPlrId()));
        }

        for (AvoidNode avoidNode : aList) {
            byteAggregator.writeBytes(Ipv6Util.byteBufForAddress(avoidNode.getAvoidNode()));
        }
    }
}
