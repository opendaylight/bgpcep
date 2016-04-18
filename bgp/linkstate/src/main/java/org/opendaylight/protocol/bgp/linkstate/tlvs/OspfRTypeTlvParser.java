/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.OspfRouteType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OspfRTypeTlvParser implements LinkstateTlvParser<OspfRouteType>, LinkstateTlvSerializer<OspfRouteType> {

    private static final Logger LOG = LoggerFactory.getLogger(OspfRTypeTlvParser.class);

    public static final int OSPF_ROUTE_TYPE = 264;

    public static final QName OSPF_ROUTE_TYPE_QNAME = QName.create(PrefixDescriptors.QNAME, "ospf-route-type").intern();

    @Override
    public void serializeTlvBody(OspfRouteType tlv, ByteBuf body, final QName qName) {
        if (tlv != null) {
            TlvUtil.writeTLV(OSPF_ROUTE_TYPE, Unpooled.wrappedBuffer(new byte[] { UnsignedBytes.checkedCast(tlv.getIntValue()) }), body);
        }
    }

    @Override
    public OspfRouteType parseTlvBody(ByteBuf value) throws BGPParsingException {
        final int rt = value.readByte();
        final OspfRouteType routeType = OspfRouteType.forValue(rt);
        if (routeType == null) {
            throw new BGPParsingException("Unknown OSPF Route Type: " + rt);
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("Parsed RouteType: {}", routeType);
        }
        return routeType;
    }
}
