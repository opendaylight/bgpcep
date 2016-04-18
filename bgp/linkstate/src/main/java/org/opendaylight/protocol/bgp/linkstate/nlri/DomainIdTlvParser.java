/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import com.google.common.primitives.UnsignedInteger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.node._case.NodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.AdvertisingNodeDescriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DomainIdTlvParser implements NlriTlvObjectParser, NlriTlvObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(DomainIdTlvParser.class);

    private static final int BGP_LS_ID = 513;

    @Override
    public void parseNlriTlvObject(final ByteBuf value, final NlriTlvTypeBuilderContext context, final NodeDescriptorsTlvBuilderParser builderparser, final NlriType nlriType) throws BGPParsingException {

        final DomainIdentifier bgpId = new DomainIdentifier(value.readUnsignedInt());
        LOG.debug("Parsed {}", bgpId);
        builderparser.setDomainIdBuilder(bgpId, context);
    }

    @Override
    public void serializeTlvObject(final ObjectType nlriTypeCase, final NlriType nlriType, final ByteBuf buffer) {
        if (nlriType.equals(NlriType.Node)) {
            final NodeDescriptors nodedescriptors = ((NodeCase)nlriTypeCase).getNodeDescriptors();
            if (nodedescriptors.getDomainId() != null) {
                TlvUtil.writeTLV(BGP_LS_ID, Unpooled.copyInt(UnsignedInteger.valueOf(nodedescriptors.getDomainId().getValue()).intValue()), buffer);
            }
        } else if (nlriType.equals(NlriType.Link)) {
            if(SimpleNlriTypeRegistry.getInstance().isLocal()) {
                final LocalNodeDescriptors localnodedesc = ((LinkCase)nlriTypeCase).getLocalNodeDescriptors();
                if (localnodedesc.getDomainId() != null) {
                    TlvUtil.writeTLV(BGP_LS_ID, Unpooled.copyInt(UnsignedInteger.valueOf(localnodedesc.getDomainId().getValue()).intValue()), buffer);
                }
            } else {
                final RemoteNodeDescriptors remnodedesc = ((LinkCase)nlriTypeCase).getRemoteNodeDescriptors();
                if (remnodedesc.getDomainId() != null) {
                    TlvUtil.writeTLV(BGP_LS_ID, Unpooled.copyInt(UnsignedInteger.valueOf(remnodedesc.getDomainId().getValue()).intValue()), buffer);
                }
            }
        } else if (nlriType.equals(NlriType.Ipv4Prefix)) {
            final AdvertisingNodeDescriptors advertnodedesc = ((PrefixCase)nlriTypeCase).getAdvertisingNodeDescriptors();
            if (advertnodedesc.getDomainId() != null) {
                TlvUtil.writeTLV(BGP_LS_ID, Unpooled.copyInt(UnsignedInteger.valueOf(advertnodedesc.getDomainId().getValue()).intValue()), buffer);
            }
        }
    }
}
