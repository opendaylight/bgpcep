/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.bgp.parser.impl.TE;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.detour.object.DetourObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.detour.object.DetourObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.detour.object.detour.object.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.detour.object.detour.object.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.detour.object.detour.object.address.family.ipv4._case.Plr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.detour.object.detour.object.address.family.ipv4._case.PlrBuilder;

public final class DetourObjectType7Parser extends AbstractBGPObjectParser {

    public static final short CLASS_NUM = 63;
    public static final short CTYPE = 7;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) {
        final DetourObjectBuilder builder = new DetourObjectBuilder();
        builder.setCType(CTYPE);
        Ipv4CaseBuilder ipv4Case = new Ipv4CaseBuilder();
        List<Plr> plrList = new ArrayList<>();
        while (byteBuf.isReadable()) {
            PlrBuilder plr = new PlrBuilder();
            plr.setPlrId(Ipv4Util.addressForByteBuf(byteBuf));
            plr.setAvoidNode(Ipv4Util.addressForByteBuf(byteBuf));
            plrList.add(plr.build());
        }
        return builder.setAddressFamily(ipv4Case.setPlr(plrList).build()).build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(teLspObject instanceof DetourObject, "DetourObject is mandatory.");
        final DetourObject detourObject = (DetourObject) teLspObject;

        final Ipv4Case plrList = (Ipv4Case) detourObject.getAddressFamily();
        final List<Plr> list = plrList.getPlr();
        serializeAttributeHeader(list.size() * 2 * IPV4_BYTES_LENGTH, CLASS_NUM, CTYPE, byteAggregator);
        for (Plr plr : list) {
            ByteBufWriteUtil.writeIpv4Address(plr.getPlrId(), byteAggregator);
            ByteBufWriteUtil.writeIpv4Address(plr.getAvoidNode(), byteAggregator);
        }
    }
}
