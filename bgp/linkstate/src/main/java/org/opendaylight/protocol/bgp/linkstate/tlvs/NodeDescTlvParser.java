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
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.node._case.NodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.AdvertisingNodeDescriptorsBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NodeDescTlvParser implements NlriTlvObjectParser, NlriTlvObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(NodeDescTlvParser.class);

    @Override
    public org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier parseNlriTlvObject(final ByteBuf buffer, final NlriType nlriType) throws BGPParsingException {

        if (nlriType.equals(NlriType.Node)) {
            final NodeDescriptorsBuilder nodeBuilder = new NodeDescriptorsBuilder();
            while (buffer.isReadable()) {
                final int type = buffer.readUnsignedShort();
                final int length = buffer.readUnsignedShort();
                final ByteBuf value = buffer.readSlice(length);
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Parsing {} Descriptor: {}", nlriType, ByteBufUtil.hexDump(value));
                }
                final Object subTlvObject = SimpleNlriTypeRegistry.getInstance().parseSubTlvObject(value, type, nlriType);
                final SubTlvNodeBuilder localBuilderObject = (SubTlvNodeBuilder) SimpleNlriTypeRegistry.getInstance().getSubTlvParser(type);
                localBuilderObject.buildNodeDescriptor(subTlvObject, nodeBuilder);
            }
            return nodeBuilder.build();
        }
        else if (nlriType.equals(NlriType.Link)) {
            final LocalNodeDescriptorsBuilder localNodeBuilder = new LocalNodeDescriptorsBuilder();
            while (buffer.isReadable()) {
                final int type = buffer.readUnsignedShort();
                final int length = buffer.readUnsignedShort();
                final ByteBuf value = buffer.readSlice(length);
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Parsing {} Descriptor: {}", nlriType, ByteBufUtil.hexDump(value));
                }
                final Object subTlvObject = SimpleNlriTypeRegistry.getInstance().parseSubTlvObject(value, type, nlriType);
                final SubTlvLinkDescBuilder localBuilderObject = (SubTlvLinkDescBuilder) SimpleNlriTypeRegistry.getInstance().getSubTlvParser(type);
                localBuilderObject.buildLocalNodeDescriptor(subTlvObject, localNodeBuilder);
            }
            return localNodeBuilder.build();
        }
        else if (nlriType.equals(NlriType.Ipv4Prefix) || nlriType.equals(NlriType.Ipv6Prefix)) {
            final AdvertisingNodeDescriptorsBuilder adverNodeBuilder = new AdvertisingNodeDescriptorsBuilder();
            while (buffer.isReadable()) {
                final int type = buffer.readUnsignedShort();
                final int length = buffer.readUnsignedShort();
                final ByteBuf value = buffer.readSlice(length);
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Parsing {} Descriptor: {}", nlriType, ByteBufUtil.hexDump(value));
                }
                final Object subTlvObject = SimpleNlriTypeRegistry.getInstance().parseSubTlvObject(value, type, nlriType);
                final SubTlvNodeBuilder localBuilderObject = (SubTlvNodeBuilder) SimpleNlriTypeRegistry.getInstance().getSubTlvParser(type);
                localBuilderObject.buildAdvertisingNodeDescriptor(subTlvObject, adverNodeBuilder);
            }
            return adverNodeBuilder.build();
        }
        throw new BGPParsingException("Nlri Type not recognized while parsing Tlvs, type: " + nlriType);
    }

    @Override
    public void serializeNlriTlvObject(final ObjectType tlvObject, final NodeIdentifier qNameId, final NlriType nlriType, final ByteBuf localdescs) {
        final SimpleNlriTypeRegistry nlriReg = SimpleNlriTypeRegistry.getInstance();
        if (nlriType.equals(NlriType.Link)) {
            nlriReg.serializeSubTlvObject(tlvObject, AsNumTlvParser.AS_NUMBER, qNameId, localdescs);
            nlriReg.serializeSubTlvObject(tlvObject, DomainIdTlvParser.BGP_LS_ID, qNameId, localdescs);
            nlriReg.serializeSubTlvObject(tlvObject, AreaIdTlvParser.AREA_ID, qNameId, localdescs);
            nlriReg.serializeSubTlvObject(tlvObject, CrouterIdTlvParser.IGP_ROUTER_ID, qNameId, localdescs);
            nlriReg.serializeSubTlvObject(tlvObject, BgpRouterIdTlvParser.BGP_ROUTER_ID, qNameId, localdescs);
            nlriReg.serializeSubTlvObject(tlvObject, MemAsNumTlvParser.MEMBER_AS_NUMBER, qNameId, localdescs);
        } else {
            nlriReg.serializeSubTlvObject(tlvObject, AsNumTlvParser.AS_NUMBER, qNameId, localdescs);
            nlriReg.serializeSubTlvObject(tlvObject, DomainIdTlvParser.BGP_LS_ID, qNameId, localdescs);
            nlriReg.serializeSubTlvObject(tlvObject, AreaIdTlvParser.AREA_ID, qNameId, localdescs);
            nlriReg.serializeSubTlvObject(tlvObject, CrouterIdTlvParser.IGP_ROUTER_ID, qNameId, localdescs);
        }
    }
}
