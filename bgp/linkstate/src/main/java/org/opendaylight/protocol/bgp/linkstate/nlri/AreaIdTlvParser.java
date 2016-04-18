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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.AreaIdentifier;
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

public final class AreaIdTlvParser implements NlriTlvObjectParser, NlriTlvObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(AreaIdTlvParser.class);

    private static final int AREA_ID = 514;

    @Override
    public void parseNlriTlvObject(final ByteBuf value, final NlriTlvTypeBuilderContext context, final NodeDescriptorsTlvBuilderParser builderparser, final NlriType nlriType) throws BGPParsingException {

        final AreaIdentifier ai = new AreaIdentifier(value.readUnsignedInt());
        LOG.debug("Parsed area identifier {}", ai);
        builderparser.setAreaIdBuilder(ai, context);
    }

    @Override
    public void serializeTlvObject(final ObjectType nlriTypeCase, final NlriType nlriType, final ByteBuf buffer) {
        if (nlriType.equals(NlriType.Node)) {
            final NodeDescriptors nodedescriptors = ((NodeCase)nlriTypeCase).getNodeDescriptors();
            if (nodedescriptors.getAreaId() != null) {
                TlvUtil.writeTLV(AREA_ID, Unpooled.copyInt(UnsignedInteger.valueOf(nodedescriptors.getAreaId().getValue()).intValue()), buffer);
            }
        } else if (nlriType.equals(NlriType.Link)) {
            if(SimpleNlriTypeRegistry.getInstance().isLocal()) {
                final LocalNodeDescriptors localnodedesc = ((LinkCase)nlriTypeCase).getLocalNodeDescriptors();
                if (localnodedesc.getAreaId() != null) {
                    TlvUtil.writeTLV(AREA_ID, Unpooled.copyInt(UnsignedInteger.valueOf(localnodedesc.getAreaId().getValue()).intValue()), buffer);
                }
            } else {
                final RemoteNodeDescriptors remnodedesc = ((LinkCase)nlriTypeCase).getRemoteNodeDescriptors();
                if (remnodedesc.getAreaId() != null) {
                    TlvUtil.writeTLV(AREA_ID, Unpooled.copyInt(UnsignedInteger.valueOf(remnodedesc.getAreaId().getValue()).intValue()), buffer);
                }
            }
        } else if (nlriType.equals(NlriType.Ipv4Prefix) || nlriType.equals(NlriType.Ipv6Prefix)) {
            final AdvertisingNodeDescriptors advertnodedesc = ((PrefixCase)nlriTypeCase).getAdvertisingNodeDescriptors();
            if (advertnodedesc.getAreaId() != null) {
                TlvUtil.writeTLV(AREA_ID, Unpooled.copyInt(UnsignedInteger.valueOf(advertnodedesc.getAreaId().getValue()).intValue()), buffer);
            }
        }
    }
}
