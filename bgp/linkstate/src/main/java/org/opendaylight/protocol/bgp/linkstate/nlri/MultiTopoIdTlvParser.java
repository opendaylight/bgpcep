/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MultiTopoIdTlvParser implements NlriTlvObjectParser, NlriTlvObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(MultiTopoIdTlvParser.class);

    @Override
    public void parseNlriTlvObject(final ByteBuf value, final NlriTlvTypeBuilderContext context, final NodeDescriptorsTlvBuilderParser builderparser, final NlriType nlriType) throws BGPParsingException {
        final LinkDescriptorsBuilder linkbuilder = context.getLinkDescriptorsBuilder();
        final PrefixDescriptorsBuilder prefixbuilder = context.getPrefixDescriptorsBuilder();
        if (nlriType.equals(NlriType.Link)) {
            final TopologyIdentifier topId = new TopologyIdentifier(value.readUnsignedShort() & TlvUtil.TOPOLOGY_ID_OFFSET);
            linkbuilder.setMultiTopologyId(topId);
            LOG.debug("Parsed link topology identifier {}.", topId);
        } else if (nlriType.equals(NlriType.Ipv4Prefix) || nlriType.equals(NlriType.Ipv6Prefix)) {
            final TopologyIdentifier topologyId = new TopologyIdentifier(value.readShort() & TlvUtil.TOPOLOGY_ID_OFFSET);
            prefixbuilder.setMultiTopologyId(topologyId);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Parsed Prefix Topology Identifier: {}", topologyId);
            }
        }
    }

    @Override
    public void serializeTlvObject(final ObjectType nlriTypeCase, final NlriType nlriType, final ByteBuf buffer) {
        if (nlriType.equals(NlriType.Link)){
            final LinkDescriptors ldescriptor = ((LinkCase)nlriTypeCase).getLinkDescriptors();
            if (ldescriptor.getMultiTopologyId() != null) {
                TlvUtil.writeTLV(TlvUtil.MULTI_TOPOLOGY_ID, Unpooled.copyShort(ldescriptor.getMultiTopologyId().getValue()), buffer);
            }
        } else if (nlriType.equals(NlriType.Ipv4Prefix)) {
            final PrefixDescriptors prefdescriptor = ((PrefixCase)nlriTypeCase).getPrefixDescriptors();
            if (prefdescriptor.getMultiTopologyId() != null) {
                TlvUtil.writeTLV(TlvUtil.MULTI_TOPOLOGY_ID, Unpooled.copyShort(prefdescriptor.getMultiTopologyId().getValue()), buffer);
            }
        }
    }
}
