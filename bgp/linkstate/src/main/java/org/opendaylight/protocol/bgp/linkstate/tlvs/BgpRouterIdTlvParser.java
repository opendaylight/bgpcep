/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.epe.rev150622.EpeNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BgpRouterIdTlvParser implements NlriSubTlvObjectParser, NlriSubTlvObjectSerializer, SubTlvLinkDescBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(BgpRouterIdTlvParser.class);

    public static final int BGP_ROUTER_ID = 516;

    @Override
    public Object parseNlriSubTlvObject(final ByteBuf value, final NlriType nlriType) throws BGPParsingException {
        final Ipv4Address bgpRouterId = Ipv4Util.addressForByteBuf(value);
        LOG.debug("Parsed BGP Router Identifier {}", bgpRouterId);
        return bgpRouterId;
    }

    @Override
    public void serializeNlriSubTlvObject(final ObjectType nlriTypeCase, final NodeIdentifier qNameId, final ByteBuf buffer) {
        if (qNameId.equals(LinkstateNlriParser.LOCAL_NODE_DESCRIPTORS_NID))  {
            final EpeNodeDescriptors epedescriptors = ((LinkCase)nlriTypeCase).getLocalNodeDescriptors();
            if (epedescriptors.getBgpRouterId() != null) {
                TlvUtil.writeTLV(BGP_ROUTER_ID, Ipv4Util.byteBufForAddress(epedescriptors.getBgpRouterId()), buffer);
            }
        } else if (qNameId.equals(LinkstateNlriParser.REMOTE_NODE_DESCRIPTORS_NID))  {
            final EpeNodeDescriptors eperemdescriptors = ((LinkCase)nlriTypeCase).getRemoteNodeDescriptors();
            if (eperemdescriptors.getBgpRouterId() != null) {
                TlvUtil.writeTLV(BGP_ROUTER_ID, Ipv4Util.byteBufForAddress(eperemdescriptors.getBgpRouterId()), buffer);
            }
        }
    }

    @Override
    public void buildLocalNodeDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((LocalNodeDescriptorsBuilder) tlvBuilder).setBgpRouterId((Ipv4Address) subTlvObject);

    }

    @Override
    public void buildRemoteNodeDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((RemoteNodeDescriptorsBuilder) tlvBuilder).setBgpRouterId((Ipv4Address) subTlvObject);

    }
}
