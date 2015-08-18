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
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.Ipv4DetourObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.Ipv4DetourObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.ipv4.detour.object.Plr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.ipv4.detour.object.PlrBuilder;

public final class DetourObjectIpv4Parser extends AbstractRSVPObjectParser {

    public static final short CLASS_NUM = 63;
    public static final short CTYPE = 7;

    @Override
    final protected RsvpTeObject localParseObject(final ByteBuf byteBuf) {
        final Ipv4DetourObjectBuilder ipv4Case = new Ipv4DetourObjectBuilder();
        final List<Plr> plrList = new ArrayList<>();
        while (byteBuf.isReadable()) {
            final PlrBuilder plr = new PlrBuilder();
            plr.setPlrId(Ipv4Util.addressForByteBuf(byteBuf));
            plr.setAvoidNode(Ipv4Util.addressForByteBuf(byteBuf));
            plrList.add(plr.build());
        }
        return ipv4Case.setPlr(plrList).build();
    }

    @Override
    final public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(teLspObject instanceof Ipv4DetourObject, "DetourObject is mandatory.");
        final Ipv4DetourObject detourObject = (Ipv4DetourObject) teLspObject;

        final List<Plr> list = detourObject.getPlr();
        serializeAttributeHeader(list.size() * 2 * Ipv4Util.IP4_LENGTH, CLASS_NUM, CTYPE, byteAggregator);
        for (Plr plr : list) {
            ByteBufWriteUtil.writeIpv4Address(plr.getPlrId(), byteAggregator);
            ByteBufWriteUtil.writeIpv4Address(plr.getAvoidNode(), byteAggregator);
        }
    }
}
