/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import org.opendaylight.protocol.bgp.linkstate.spi.LinkstateTlvParser;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.OspfRouteType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;

public final class OspfRouteTlvParser implements LinkstateTlvParser<OspfRouteType>, LinkstateTlvParser.LinkstateTlvSerializer<OspfRouteType> {

    public static final QName OSPF_ROUTE_TYPE_QNAME = QName.create(PrefixDescriptors.QNAME, "ospf-route-type").intern();
    public static final NodeIdentifier OSPF_ROUTE_NID = new NodeIdentifier(OspfRouteTlvParser.OSPF_ROUTE_TYPE_QNAME);
    private static final int OSPF_ROUTE_TYPE = 264;

    @Override
    public void serializeTlvBody(final OspfRouteType tlv, final ByteBuf body) {
        ByteBufWriteUtil.writeUnsignedByte((short) tlv.getIntValue(), body);
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
        final Optional<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> ospfRoute = prefixDesc.getChild(OSPF_ROUTE_NID);
        if (ospfRoute.isPresent()) {
            return OspfRouteType.forValue(domOspfRouteTypeValue((String) ospfRoute.get().getValue()));
        }
        return null;
    }


    // FIXME : use codec
    private static int domOspfRouteTypeValue(final String ospfRouteType) {
        switch (ospfRouteType) {
        case "intra-area":
            return OspfRouteType.IntraArea.getIntValue();
        case "inter-area":
            return OspfRouteType.InterArea.getIntValue();
        case "external1":
            return OspfRouteType.External1.getIntValue();
        case "external2":
            return OspfRouteType.External2.getIntValue();
        case "nssa1":
            return OspfRouteType.Nssa1.getIntValue();
        case "nssa2":
            return OspfRouteType.Nssa2.getIntValue();
        default:
            return 0;
        }
    }
}
