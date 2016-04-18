/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

import com.google.common.primitives.UnsignedInteger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.node._case.NodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.node._case.NodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.AdvertisingNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.AdvertisingNodeDescriptorsBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AsNumTlvParser implements NlriSubTlvObjectParser, NlriSubTlvObjectSerializer, SubTlvNodeBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(AsNumTlvParser.class);

    public static final int AS_NUMBER = 512;

    @Override
    public Object parseNlriSubTlvObject(final ByteBuf value, final NlriType nlriType) throws BGPParsingException {
        final AsNumber asnumber = new AsNumber(value.readUnsignedInt());
        LOG.debug("Parsed {}", asnumber);
        return asnumber;
    }

    @Override
    public void serializeNlriSubTlvObject(final ObjectType nlriTypeCase, final NodeIdentifier qNameId, final ByteBuf buffer) {
        if (qNameId.equals(LinkstateNlriParser.NODE_DESCRIPTORS_NID)) {
            final NodeDescriptors nodeDescriptors = ((NodeCase)nlriTypeCase).getNodeDescriptors();
            if (nodeDescriptors.getAsNumber() != null) {
                TlvUtil.writeTLV(AS_NUMBER, Unpooled.copyInt(UnsignedInteger.valueOf(nodeDescriptors.getAsNumber().getValue()).intValue()), buffer);
            }
        } else if (qNameId.equals(LinkstateNlriParser.LOCAL_NODE_DESCRIPTORS_NID)) {
            final LocalNodeDescriptors localNodeDesc = ((LinkCase)nlriTypeCase).getLocalNodeDescriptors();
            if (localNodeDesc.getAsNumber() != null) {
                TlvUtil.writeTLV(AS_NUMBER, Unpooled.copyInt(UnsignedInteger.valueOf(localNodeDesc.getAsNumber().getValue()).intValue()), buffer);
            }
        } else if (qNameId.equals(LinkstateNlriParser.REMOTE_NODE_DESCRIPTORS_NID))  {
            final RemoteNodeDescriptors remNodeDesc = ((LinkCase)nlriTypeCase).getRemoteNodeDescriptors();
            if (remNodeDesc.getAsNumber() != null) {
                TlvUtil.writeTLV(AS_NUMBER, Unpooled.copyInt(UnsignedInteger.valueOf(remNodeDesc.getAsNumber().getValue()).intValue()), buffer);
            }
        } else if (qNameId.equals(LinkstateNlriParser.ADVERTISING_NODE_DESCRIPTORS_NID)) {
            final AdvertisingNodeDescriptors advertNodeDesc = ((PrefixCase)nlriTypeCase).getAdvertisingNodeDescriptors();
            if (advertNodeDesc.getAsNumber() != null) {
                TlvUtil.writeTLV(AS_NUMBER, Unpooled.copyInt(UnsignedInteger.valueOf(advertNodeDesc.getAsNumber().getValue()).intValue()), buffer);
            }
        }
    }

    @Override
    public void buildNodeDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((NodeDescriptorsBuilder) tlvBuilder).setAsNumber((AsNumber) subTlvObject);

    }

    @Override
    public void buildLocalNodeDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((LocalNodeDescriptorsBuilder) tlvBuilder).setAsNumber((AsNumber) subTlvObject);

    }

    @Override
    public void buildRemoteNodeDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((RemoteNodeDescriptorsBuilder) tlvBuilder).setAsNumber((AsNumber) subTlvObject);

    }

    @Override
    public void buildAdvertisingNodeDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((AdvertisingNodeDescriptorsBuilder) tlvBuilder).setAsNumber((AsNumber) subTlvObject);

    }
}
