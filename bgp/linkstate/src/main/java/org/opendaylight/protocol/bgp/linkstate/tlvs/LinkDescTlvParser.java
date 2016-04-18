/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.opendaylight.protocol.bgp.linkstate.nlri.SimpleNlriTypeRegistry;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class LinkDescTlvParser implements NlriTlvObjectParser, NlriTlvObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(LinkDescTlvParser.class);

    @Override
    public LinkDescriptors parseNlriTlvObject(final ByteBuf buffer, final NlriType nlriType) throws BGPParsingException {

        final LinkDescriptorsBuilder linkBuilder = new LinkDescriptorsBuilder();
        final SimpleNlriTypeRegistry nlriReg = SimpleNlriTypeRegistry.getInstance();
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            final ByteBuf value = buffer.readSlice(length);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Parsing {} Descriptor: {}", nlriType, ByteBufUtil.hexDump(value));
            }
            if (type == LinkLrIdTlvParser.LINK_LR_IDENTIFIERS) {
                LinkLrIdTlvParser.parseLinkLrIdSubTlvObject(value, linkBuilder);
            } else {
                final Object subTlvObject = nlriReg.parseSubTlvObject(value, type, nlriType);
                final SubTlvLinkBuilder linkBuilderObject = (SubTlvLinkBuilder) nlriReg.getSubTlvParser(type);
                linkBuilderObject.buildLinkDescriptor(subTlvObject, linkBuilder);
            }
        }
        return linkBuilder.build();
    }

    @Override
    public void serializeNlriTlvObject(final ObjectType tlvObject, final NodeIdentifier qNameId, final NlriType nlriType, final ByteBuf localDescs) {
        final SimpleNlriTypeRegistry nlriReg = SimpleNlriTypeRegistry.getInstance();
        nlriReg.serializeSubTlvObject(tlvObject, LinkLrIdTlvParser.LINK_LR_IDENTIFIERS, qNameId, localDescs);
        nlriReg.serializeSubTlvObject(tlvObject, Ipv4IfaceAddTlvParser.IPV4_IFACE_ADDRESS, qNameId, localDescs);
        nlriReg.serializeSubTlvObject(tlvObject, Ipv4NeighborAddTlvParser.IPV4_NEIGHBOR_ADDRESS, qNameId, localDescs);
        nlriReg.serializeSubTlvObject(tlvObject, Ipv6IFaceAddTlvParser.IPV6_IFACE_ADDRESS, qNameId, localDescs);
        nlriReg.serializeSubTlvObject(tlvObject, Ipv6NeighborAddTlvParser.IPV6_NEIGHBOR_ADDRESS, qNameId, localDescs);
        nlriReg.serializeSubTlvObject(tlvObject, TlvUtil.MULTI_TOPOLOGY_ID, qNameId, localDescs);
    }
}
