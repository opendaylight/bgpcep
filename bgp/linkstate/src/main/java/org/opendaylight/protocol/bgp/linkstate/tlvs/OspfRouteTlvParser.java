/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.OspfRouteType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yangtools.yang.common.QName;

public final class OspfRouteTlvParser implements LinkstateTlvParser<OspfRouteType>, LinkstateTlvSerializer<OspfRouteType> {

    private static final int OSPF_ROUTE_TYPE = 264;

    public static final QName OSPF_ROUTE_TYPE_QNAME = QName.create(PrefixDescriptors.QNAME, "ospf-route-type").intern();

    @Override
    public void serializeTlvBody(final OspfRouteType tlv, final ByteBuf body) {
        ByteBufWriteUtil.writeUnsignedByte((short) tlv.getIntValue(), body);
    }

    @Override
    public OspfRouteType parseTlvBody(final ByteBuf value) {
        return OspfRouteType.forValue(value.readUnsignedByte());
    }

    @Override
    public int getType() {
        return OSPF_ROUTE_TYPE;
    }

    @Override
    public QName getTlvQName() {
        return OSPF_ROUTE_TYPE_QNAME;
    }
}
