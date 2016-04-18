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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
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

public final class AsNumTlvParser implements NlriTlvObjectParser, NlriTlvObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(AsNumTlvParser.class);

    private static final int AS_NUMBER = 512;

    @Override
    public void parseNlriTlvObject(final ByteBuf value, final NlriTlvTypeBuilderContext context, final NodeDescriptorsTlvBuilderParser builderparser, final NlriType nlriType) throws BGPParsingException {
        final AsNumber asnumber = new AsNumber(value.readUnsignedInt());
        LOG.debug("Parsed {}", asnumber);
        builderparser.setAsNumBuilder(asnumber, context);
    }

    @Override
    public void serializeTlvObject(final ObjectType nlriTypeCase, final NlriType nlriType, final ByteBuf buffer) {
        if (nlriType.equals(NlriType.Node)) {
            final NodeDescriptors nodedescriptors = ((NodeCase)nlriTypeCase).getNodeDescriptors();
            if (nodedescriptors.getAsNumber() != null) {
                TlvUtil.writeTLV(AS_NUMBER, Unpooled.copyInt(UnsignedInteger.valueOf(nodedescriptors.getAsNumber().getValue()).intValue()), buffer);
            }
        } else if (nlriType.equals(NlriType.Link)) {
            if(SimpleNlriTypeRegistry.getInstance().isLocal()) {
                final LocalNodeDescriptors localnodedesc = ((LinkCase)nlriTypeCase).getLocalNodeDescriptors();
                if (localnodedesc.getAsNumber() != null) {
                    TlvUtil.writeTLV(AS_NUMBER, Unpooled.copyInt(UnsignedInteger.valueOf(localnodedesc.getAsNumber().getValue()).intValue()), buffer);
                }
            } else {
                final RemoteNodeDescriptors remnodedesc = ((LinkCase)nlriTypeCase).getRemoteNodeDescriptors();
                if (remnodedesc.getAsNumber() != null) {
                    TlvUtil.writeTLV(AS_NUMBER, Unpooled.copyInt(UnsignedInteger.valueOf(remnodedesc.getAsNumber().getValue()).intValue()), buffer);
                }
            }
        } else if (nlriType.equals(NlriType.Ipv4Prefix) || nlriType.equals(NlriType.Ipv6Prefix)) {
            final AdvertisingNodeDescriptors advertnodedesc = ((PrefixCase)nlriTypeCase).getAdvertisingNodeDescriptors();
            if (advertnodedesc.getAsNumber() != null) {
                TlvUtil.writeTLV(AS_NUMBER, Unpooled.copyInt(UnsignedInteger.valueOf(advertnodedesc.getAsNumber().getValue()).intValue()), buffer);
            }
        }
    }
}
