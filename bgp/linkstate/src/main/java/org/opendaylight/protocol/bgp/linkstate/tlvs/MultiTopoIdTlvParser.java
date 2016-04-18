/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptorsBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MultiTopoIdTlvParser implements NlriSubTlvObjectParser, NlriSubTlvObjectSerializer, SubTlvLinkBuilder, SubTlvPrefixBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(MultiTopoIdTlvParser.class);

    @Override
    public Object parseNlriSubTlvObject(final ByteBuf value, final NlriType nlriType) throws BGPParsingException {
        if (nlriType.equals(NlriType.Link)) {
            final TopologyIdentifier topId = new TopologyIdentifier(value.readUnsignedShort() & TlvUtil.TOPOLOGY_ID_OFFSET);
            LOG.debug("Parsed {} topology identifier {}.", nlriType, topId);
            return topId;
        } else if (nlriType.equals(NlriType.Ipv4Prefix) || nlriType.equals(NlriType.Ipv6Prefix)) {
            final TopologyIdentifier topologyId = new TopologyIdentifier(value.readShort() & TlvUtil.TOPOLOGY_ID_OFFSET);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Parsed {} Topology Identifier: {}", nlriType, topologyId);
            }
            return topologyId;
        }
        return null;
    }

    @Override
    public void serializeNlriSubTlvObject(final ObjectType nlriTypeCase, final NodeIdentifier qNameId, final ByteBuf buffer) {
        if (qNameId.equals(LinkstateNlriParser.LINK_DESCRIPTORS_NID)){
            final LinkDescriptors ldescriptor = ((LinkCase)nlriTypeCase).getLinkDescriptors();
            if (ldescriptor.getMultiTopologyId() != null) {
                TlvUtil.writeTLV(TlvUtil.MULTI_TOPOLOGY_ID, Unpooled.copyShort(ldescriptor.getMultiTopologyId().getValue()), buffer);
            }
        } else if (qNameId.equals(LinkstateNlriParser.PREFIX_DESCRIPTORS_NID)) {
            final PrefixDescriptors prefdescriptor = ((PrefixCase)nlriTypeCase).getPrefixDescriptors();
            if (prefdescriptor.getMultiTopologyId() != null) {
                TlvUtil.writeTLV(TlvUtil.MULTI_TOPOLOGY_ID, Unpooled.copyShort(prefdescriptor.getMultiTopologyId().getValue()), buffer);
            }
        }
    }

    @Override
    public void buildLinkDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((LinkDescriptorsBuilder) tlvBuilder).setMultiTopologyId((TopologyIdentifier) subTlvObject);
    }

    @Override
    public void buildPrefixDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((PrefixDescriptorsBuilder) tlvBuilder).setMultiTopologyId((TopologyIdentifier) subTlvObject);

    }
}
