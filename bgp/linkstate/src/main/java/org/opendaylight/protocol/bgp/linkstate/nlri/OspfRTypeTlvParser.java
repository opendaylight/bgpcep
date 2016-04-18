/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.OspfRouteType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptorsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OspfRTypeTlvParser implements NlriTlvObjectParser, NlriTlvObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(OspfRTypeTlvParser.class);

    private static final int OSPF_ROUTE_TYPE = 264;

    @Override
    public void parseNlriTlvObject(final ByteBuf value, final NlriTlvTypeBuilderContext context, final NodeDescriptorsTlvBuilderParser builderparser, final NlriType nlriType) throws BGPParsingException {
        final PrefixDescriptorsBuilder prefixbuilder = context.getPrefixDescriptorsBuilder();
        final int rt = value.readByte();
        final OspfRouteType routeType = OspfRouteType.forValue(rt);
        if (routeType == null) {
            throw new BGPParsingException("Unknown OSPF Route Type: " + rt);
        }
        prefixbuilder.setOspfRouteType(routeType);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Parser RouteType: {}", routeType);
        }
    }

    @Override
    public void serializeTlvObject(final ObjectType nlriTypeCase, final NlriType nlriType, final ByteBuf buffer) {
        final PrefixDescriptors prefdescriptor = ((PrefixCase)nlriTypeCase).getPrefixDescriptors();
        if (prefdescriptor.getOspfRouteType() != null) {
            TlvUtil.writeTLV(OSPF_ROUTE_TYPE, Unpooled.wrappedBuffer(new byte[] { UnsignedBytes.checkedCast(prefdescriptor.getOspfRouteType().getIntValue()) }), buffer);
        }
    }
}
