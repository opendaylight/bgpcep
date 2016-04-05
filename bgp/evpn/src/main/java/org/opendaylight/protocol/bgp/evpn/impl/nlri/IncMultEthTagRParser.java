/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import static org.opendaylight.bgp.concepts.RouteDistinguisherUtil.parseRouteDistinguisher;
import static org.opendaylight.bgp.concepts.RouteDistinguisherUtil.serializeRouteDistinquisher;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.IncMultiEthernetTagRes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.ethernet.tag.id.EthernetTagId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.ethernet.tag.id.EthernetTagIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.Evpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.IncMultiEthernetTagResCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.evpn.IncMultiEthernetTagResCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public final class IncMultEthTagRParser extends AbstractEvpnNlri {
    static final NodeIdentifier INC_MULT_ROUTE_CASE_NID = new NodeIdentifier(IncMultiEthernetTagRes.QNAME);
    private static final int CONTENT_LENGTH = 17;
    private static final int CONTENT_LENGTH2 = 29;
    private static final NodeIdentifier INC_MULT_ROUTE_NID = new NodeIdentifier(IncMultiEthernetTagRes.QNAME);

    @Override
    public Evpn parseEvpn(final ByteBuf buffer) {
        Preconditions.checkArgument(buffer.readableBytes() == CONTENT_LENGTH || buffer.readableBytes() == CONTENT_LENGTH2,
            "Wrong length of array of bytes. Passed: " + buffer.readableBytes() + ";");

        final RouteDistinguisher rd = parseRouteDistinguisher(buffer);
        final EthernetTagId eti = new EthernetTagIdBuilder().setVlanId(buffer.readUnsignedInt()).build();
        IpAddress ip = Preconditions.checkNotNull(parseIpAddress(buffer));
        return new IncMultiEthernetTagResCaseBuilder().setRouteDistinguisher(rd).setEthernetTagId(eti).setOrigRouteIp(ip).build();
    }

    @Override
    public void serializeEvpn(final Evpn evpn, final ByteBuf buffer) {
        Preconditions.checkArgument(evpn instanceof IncMultiEthernetTagResCase, "Unknown evpn instance. Passed %s. Needed IncMultiEthernetTagResCase.",
            evpn.getClass());
        serialize(NlriType.IncMultEthTag.getIntValue(), serializeBody((IncMultiEthernetTagResCase) evpn), buffer);
    }

    @Override
    public Evpn serializeEvpnModel(final ChoiceNode evpnCase) {
        final ContainerNode evpn = (ContainerNode) evpnCase.getChild(INC_MULT_ROUTE_NID);
        final IncMultiEthernetTagResCaseBuilder builder = new IncMultiEthernetTagResCaseBuilder();
        builder.setRouteDistinguisher(extractRouteDistinguisher(evpn));
        builder.setEthernetTagId(extractETI(evpn));
        builder.setOrigRouteIp(extractOrigRouteIp(evpn));
        return builder.build();
    }

    private ByteBuf serializeBody(final IncMultiEthernetTagResCase evpn) {
        final ByteBuf body = Unpooled.buffer();
        serializeRouteDistinquisher(evpn.getRouteDistinguisher(), body);
        ByteBufWriteUtil.writeUnsignedInt(evpn.getEthernetTagId().getVlanId(), body);
        final ByteBuf orig = serializeIpAddress(evpn.getOrigRouteIp());
        Preconditions.checkArgument(orig.readableBytes() > 0);
        body.writeBytes(orig);
        return body;
    }
}
