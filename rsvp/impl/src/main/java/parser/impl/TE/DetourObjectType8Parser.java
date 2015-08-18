/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package parser.impl.TE;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.DetourObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.DetourObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.address.family.ipv6._case.AvoidNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.address.family.ipv6._case.AvoidNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.address.family.ipv6._case.PlrId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.detour.object.detour.object.address.family.ipv6._case.PlrIdBuilder;

public final class DetourObjectType8Parser extends AbstractBGPObjectParser {

    public static final short CLASS_NUM = 63;
    public static final short CTYPE = 8;

    @Override
    protected RsvpTeObject localParseObject(final ByteBuf byteBuf) {
        final DetourObjectBuilder builder = new DetourObjectBuilder();
        builder.setCType(CTYPE);
        final ByteBuf plr_id = byteBuf.readSlice(byteBuf.capacity() / 2);
        Ipv6CaseBuilder ipv6Case = new Ipv6CaseBuilder();

        final List<PlrId> plrIdList = new ArrayList<>();

        while (plr_id.isReadable()) {
            PlrIdBuilder plr = new PlrIdBuilder();
            plr.setPlrId(Ipv6Util.addressForByteBuf(plr_id));
            plrIdList.add(plr.build());
        }

        final List<AvoidNode> avoidNodeList = new ArrayList<>();
        while (byteBuf.isReadable()) {
            AvoidNodeBuilder plr = new AvoidNodeBuilder();
            plr.setAvoidNode(Ipv6Util.addressForByteBuf(byteBuf));
            avoidNodeList.add(plr.build());
        }

        return builder.setAddressFamily(ipv6Case.setPlrId(plrIdList).setAvoidNode(avoidNodeList).build()).build();
    }

    @Override
    public void localSerializeObject(final RsvpTeObject teLspObject, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(teLspObject instanceof DetourObject, "DetourObject is mandatory.");
        final DetourObject detourObject = (DetourObject) teLspObject;

        final Ipv6Case plrList = (Ipv6Case) detourObject.getAddressFamily();
        final List<PlrId> pList = plrList.getPlrId();
        final List<AvoidNode> aList = plrList.getAvoidNode();
        serializeAttributeHeader((pList.size() * IPV6_BYTES_LENGTH) + (aList.size() * IPV6_BYTES_LENGTH), CLASS_NUM, CTYPE, byteAggregator);

        for (PlrId plrId : pList) {
            byteAggregator.writeBytes(Ipv6Util.byteBufForAddress(plrId.getPlrId()));
        }

        for (AvoidNode avoidNode : aList) {
            byteAggregator.writeBytes(Ipv6Util.byteBufForAddress(avoidNode.getAvoidNode()));
        }
    }
}
