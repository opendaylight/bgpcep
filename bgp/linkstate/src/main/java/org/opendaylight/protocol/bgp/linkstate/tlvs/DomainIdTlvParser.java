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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.DomainIdentifier;
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

public final class DomainIdTlvParser implements NlriSubTlvObjectParser, NlriSubTlvObjectSerializer, SubTlvNodeBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(DomainIdTlvParser.class);

    public static final int BGP_LS_ID = 513;

    @Override
    public Object parseNlriSubTlvObject(final ByteBuf value, final NlriType nlriType) throws BGPParsingException {

        final DomainIdentifier bgpId = new DomainIdentifier(value.readUnsignedInt());
        LOG.debug("Parsed {}", bgpId);
        return bgpId;
    }

    @Override
    public void serializeNlriSubTlvObject(final ObjectType nlriTypeCase, final NodeIdentifier qNameId, final ByteBuf buffer) {
        if (qNameId.equals(LinkstateNlriParser.NODE_DESCRIPTORS_NID)) {
            final NodeDescriptors nodeDescriptors = ((NodeCase)nlriTypeCase).getNodeDescriptors();
            if (nodeDescriptors.getDomainId() != null) {
                TlvUtil.writeTLV(BGP_LS_ID, Unpooled.copyInt(UnsignedInteger.valueOf(nodeDescriptors.getDomainId().getValue()).intValue()), buffer);
            }
        } else if (qNameId.equals(LinkstateNlriParser.LOCAL_NODE_DESCRIPTORS_NID)) {
            final LocalNodeDescriptors localNodeDesc = ((LinkCase)nlriTypeCase).getLocalNodeDescriptors();
            if (localNodeDesc.getDomainId() != null) {
                TlvUtil.writeTLV(BGP_LS_ID, Unpooled.copyInt(UnsignedInteger.valueOf(localNodeDesc.getDomainId().getValue()).intValue()), buffer);
            }
        } else if (qNameId.equals(LinkstateNlriParser.REMOTE_NODE_DESCRIPTORS_NID)) {
            final RemoteNodeDescriptors remNodeDesc = ((LinkCase)nlriTypeCase).getRemoteNodeDescriptors();
            if (remNodeDesc.getDomainId() != null) {
                TlvUtil.writeTLV(BGP_LS_ID, Unpooled.copyInt(UnsignedInteger.valueOf(remNodeDesc.getDomainId().getValue()).intValue()), buffer);
            }
        } else if (qNameId.equals(LinkstateNlriParser.ADVERTISING_NODE_DESCRIPTORS_NID)) {
            final AdvertisingNodeDescriptors advertNodeDesc = ((PrefixCase)nlriTypeCase).getAdvertisingNodeDescriptors();
            if (advertNodeDesc.getDomainId() != null) {
                TlvUtil.writeTLV(BGP_LS_ID, Unpooled.copyInt(UnsignedInteger.valueOf(advertNodeDesc.getDomainId().getValue()).intValue()), buffer);
            }
        }
    }

    @Override
    public void buildNodeDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((NodeDescriptorsBuilder) tlvBuilder).setDomainId((DomainIdentifier) subTlvObject);

    }

    @Override
    public void buildLocalNodeDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((LocalNodeDescriptorsBuilder) tlvBuilder).setDomainId((DomainIdentifier) subTlvObject);

    }

    @Override
    public void buildRemoteNodeDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((RemoteNodeDescriptorsBuilder) tlvBuilder).setDomainId((DomainIdentifier) subTlvObject);

    }

    @Override
    public void buildAdvertisingNodeDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((AdvertisingNodeDescriptorsBuilder) tlvBuilder).setDomainId((DomainIdentifier) subTlvObject);

    }
}
