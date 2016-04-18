/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.epe.rev150622.EpeNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BgpRouterIdTlvParser implements NlriTlvObjectParser, NlriTlvObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(BgpRouterIdTlvParser.class);

    private static final int BGP_ROUTER_ID = 516;

    @Override
    public void parseNlriTlvObject(final ByteBuf value, final NlriTlvTypeBuilderContext context, final NodeDescriptorsTlvBuilderParser builderparser, final NlriType nlriType) throws BGPParsingException {
        final LinkNlriParser linkparser = new LinkNlriParser();
        final Ipv4Address bgpRouterId = Ipv4Util.addressForByteBuf(value);
        LOG.debug("Parsed BGP Router Identifier {}", bgpRouterId);
        linkparser.setBgpRidBuilder(bgpRouterId, context);
    }

    @Override
    public void serializeTlvObject(final ObjectType nlriTypeCase, final NlriType nlriType, final ByteBuf buffer) {
        if (SimpleNlriTypeRegistry.getInstance().isLocal()) {
            final EpeNodeDescriptors epedescriptors = ((LinkCase)nlriTypeCase).getLocalNodeDescriptors();
            if (epedescriptors.getBgpRouterId() != null) {
                TlvUtil.writeTLV(BGP_ROUTER_ID, Ipv4Util.byteBufForAddress(epedescriptors.getBgpRouterId()), buffer);
            }
        } else {
            final EpeNodeDescriptors eperemdescriptors = ((LinkCase)nlriTypeCase).getRemoteNodeDescriptors();
            if (eperemdescriptors.getBgpRouterId() != null) {
                TlvUtil.writeTLV(BGP_ROUTER_ID, Ipv4Util.byteBufForAddress(eperemdescriptors.getBgpRouterId()), buffer);
            }
        }
    }
}
