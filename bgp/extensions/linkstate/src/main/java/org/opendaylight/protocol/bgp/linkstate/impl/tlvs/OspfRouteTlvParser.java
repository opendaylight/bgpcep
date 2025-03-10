/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.LinkstateTlvParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.OspfRouteType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public final class OspfRouteTlvParser implements LinkstateTlvParser<OspfRouteType>,
        LinkstateTlvParser.LinkstateTlvSerializer<OspfRouteType> {
    public static final QName OSPF_ROUTE_TYPE_QNAME = QName.create(PrefixDescriptors.QNAME, "ospf-route-type").intern();
    public static final NodeIdentifier OSPF_ROUTE_NID = NodeIdentifier.create(OspfRouteTlvParser.OSPF_ROUTE_TYPE_QNAME);

    private static final int OSPF_ROUTE_TYPE = 264;

    @Override
    public void serializeTlvBody(final OspfRouteType tlv, final ByteBuf body) {
        body.writeByte(tlv.getIntValue());
    }

    @Override
    public int getType() {
        return OSPF_ROUTE_TYPE;
    }

    @Override
    public OspfRouteType parseTlvBody(final ByteBuf value) {
        return OspfRouteType.forValue(value.readUnsignedByte());
    }

    @Override
    public QName getTlvQName() {
        return OSPF_ROUTE_TYPE_QNAME;
    }

    public static OspfRouteType serializeModel(final ContainerNode prefixDesc) {
        final var ospfRoute = prefixDesc.childByArg(OSPF_ROUTE_NID);
        return ospfRoute == null ? null : OspfRouteType.forValue(domOspfRouteTypeValue((String) ospfRoute.body()));
    }

    // FIXME : use codec
    private static int domOspfRouteTypeValue(final String ospfRouteType) {
        return switch (ospfRouteType) {
            case "intra-area" -> OspfRouteType.IntraArea.getIntValue();
            case "inter-area" -> OspfRouteType.InterArea.getIntValue();
            case "external1" -> OspfRouteType.External1.getIntValue();
            case "external2" -> OspfRouteType.External2.getIntValue();
            case "nssa1" -> OspfRouteType.Nssa1.getIntValue();
            case "nssa2" -> OspfRouteType.Nssa2.getIntValue();
            default -> 0;
        };
    }
}
