/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.linkstate.tlvs.Ipv4IfaceAddTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.Ipv4NeighborAddTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.Ipv6IFaceAddTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.Ipv6NeighborAddTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.LocalNodeDescriptorTlvC;
import org.opendaylight.protocol.bgp.linkstate.tlvs.MultiTopoIdTlvParser;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Ipv6InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkLrIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptors;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public final class LinkNlriParser implements NlriTypeCaseParser, NlriTypeCaseSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(LinkNlriParser.class);

    final SimpleNlriTypeRegistry nlriTypeReg = SimpleNlriTypeRegistry.getInstance();

    /* Link Descriptor QNames */
    @VisibleForTesting
    public static final NodeIdentifier IPV4_IFACE_NID = new NodeIdentifier(QName.create(LinkDescriptors.QNAME, "ipv4-interface-address").intern());
    private static final NodeIdentifier IPV6_IFACE_NID = new NodeIdentifier(QName.create(LinkDescriptors.QNAME, "ipv6-interface-address").intern());
    @VisibleForTesting
    public static final NodeIdentifier IPV4_NEIGHBOR_NID = new NodeIdentifier(QName.create(LinkDescriptors.QNAME, "ipv4-neighbor-address").intern());
    private static final NodeIdentifier IPV6_NEIGHBOR_NID = new NodeIdentifier(QName.create(LinkDescriptors.QNAME, "ipv6-neighbor-address").intern());
    @VisibleForTesting
    public static final NodeIdentifier LINK_LOCAL_NID = new NodeIdentifier(QName.create(LinkDescriptors.QNAME, "link-local-identifier").intern());
    @VisibleForTesting
    public static final NodeIdentifier LINK_REMOTE_NID = new NodeIdentifier(QName.create(LinkDescriptors.QNAME, "link-remote-identifier").intern());

    static LinkDescriptors serializeLinkDescriptors(final ContainerNode descriptors) {
        final LinkDescriptorsBuilder linkDescBuilder = new LinkDescriptorsBuilder();

        if (descriptors.getChild(LINK_LOCAL_NID).isPresent() && descriptors.getChild(LINK_REMOTE_NID).isPresent()) {
            linkDescBuilder.setLinkLocalIdentifier((Long) descriptors.getChild(LINK_LOCAL_NID).get().getValue());
            linkDescBuilder.setLinkRemoteIdentifier((Long) descriptors.getChild(LINK_REMOTE_NID).get().getValue());
        }
        if (descriptors.getChild(IPV4_IFACE_NID).isPresent()) {
            linkDescBuilder.setIpv4InterfaceAddress(new Ipv4InterfaceIdentifier((String) descriptors.getChild(IPV4_IFACE_NID).get().getValue()));
        }
        if (descriptors.getChild(IPV6_IFACE_NID).isPresent()) {
            linkDescBuilder.setIpv6InterfaceAddress(new Ipv6InterfaceIdentifier((String) descriptors.getChild(IPV6_IFACE_NID).get().getValue()));
        }
        if (descriptors.getChild(IPV4_NEIGHBOR_NID).isPresent()) {
            linkDescBuilder.setIpv4NeighborAddress(new Ipv4InterfaceIdentifier((String) descriptors.getChild(IPV4_NEIGHBOR_NID).get().getValue()));
        }
        if (descriptors.getChild(IPV6_NEIGHBOR_NID).isPresent()) {
            linkDescBuilder.setIpv6NeighborAddress(new Ipv6InterfaceIdentifier((String) descriptors.getChild(IPV6_NEIGHBOR_NID).get().getValue()));
        }
        if (descriptors.getChild(TlvUtil.MULTI_TOPOLOGY_NID).isPresent()) {
            linkDescBuilder.setMultiTopologyId(new TopologyIdentifier((Integer) descriptors.getChild(TlvUtil.MULTI_TOPOLOGY_NID).get().getValue()));
        }
        return linkDescBuilder.build();
    }

    private LinkIdentifier parseTlvBody(final ByteBuf value) throws BGPParsingException {
        //parse Link sub-TLVs
        final Long linkLocalIdentifier = value.readUnsignedInt();
        final Long linkRemoteIdentifier = value.readUnsignedInt();
        LOG.debug("Parsed link local {} remote {} Identifiers.", linkLocalIdentifier, linkRemoteIdentifier);
        final Ipv4InterfaceIdentifier ipv4IntAdd = nlriTypeReg.parseSubTlv(value);
        final Ipv4InterfaceIdentifier ipv4NeighborAdd = nlriTypeReg.parseSubTlv(value);
        final Ipv6InterfaceIdentifier ipv6IntAdd = nlriTypeReg.parseSubTlv(value);
        final Ipv6InterfaceIdentifier ipv6NeighborAdd = nlriTypeReg.parseSubTlv(value);
        final TopologyIdentifier topoId = nlriTypeReg.parseSubTlv(value);
        return new LinkIdentifier() {
            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return LinkIdentifier.class;
            }
            @Override
            public Long getLinkLocalIdentifier() {
                return linkLocalIdentifier;
            }
            @Override
            public Long getLinkRemoteIdentifier() {
                return linkRemoteIdentifier;
            }
            @Override
            public Ipv4InterfaceIdentifier getIpv4InterfaceAddress() {
                return ipv4IntAdd;
            }
            @Override
            public Ipv4InterfaceIdentifier getIpv4NeighborAddress() {
                return ipv4NeighborAdd;
            }
            @Override
            public Ipv6InterfaceIdentifier getIpv6InterfaceAddress() {
                return ipv6IntAdd;
            }
            @Override
            public Ipv6InterfaceIdentifier getIpv6NeighborAddress() {
                return ipv6NeighborAdd;
            }
            @Override
            public TopologyIdentifier getMultiTopologyId() {
                return topoId;
            }
        };
    }

    @Override
    public ObjectType parseTypeNlri(final ByteBuf nlri, final NlriType type, final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier localDescriptor, final ByteBuf restNlri) throws BGPParsingException {

        final int nodetype = restNlri.readUnsignedShort();
        final int length = restNlri.readUnsignedShort();
        final SimpleNlriTypeRegistry nlriTypeReg = SimpleNlriTypeRegistry.getInstance();
        RemoteNodeDescriptors remoteDescriptors = null;
        if (nodetype == LocalNodeDescriptorTlvC.REMOTE_NODE_DESCRIPTORS_TYPE) {
            remoteDescriptors = nlriTypeReg.parseTlv(restNlri.readSlice(length), nodetype, type);
        }
        LinkIdentifier linkDescriptor = parseTlvBody(restNlri.slice());
        LinkCaseBuilder linkbuilder = new LinkCaseBuilder();
        LinkCase linkcase = linkbuilder.setLocalNodeDescriptors((LocalNodeDescriptors) localDescriptor).setRemoteNodeDescriptors((RemoteNodeDescriptors) remoteDescriptors).setLinkDescriptors(new LinkDescriptorsBuilder(linkDescriptor).build()).build();

        return linkcase;
    }

    private void serializeLinkDescTlvBody(final LinkIdentifier tlv, final ByteBuf body) {
        //serialize Link sub-TLVs
        nlriTypeReg.serializeTlv(LinkLrIdentifiers.QNAME, tlv, body);
        nlriTypeReg.serializeTlv(Ipv4IfaceAddTlvParser.IPV4_IFACE_ADDRESS_QNAME, tlv.getIpv4InterfaceAddress(), body);
        nlriTypeReg.serializeTlv(Ipv4NeighborAddTlvParser.IPV4_NEIGHBOR_ADDRESS_QNAME, tlv.getIpv4NeighborAddress(), body);
        nlriTypeReg.serializeTlv(Ipv6IFaceAddTlvParser.IPV6_IFACE_ADDRESS_QNAME, tlv.getIpv6InterfaceAddress(), body);
        nlriTypeReg.serializeTlv(Ipv6NeighborAddTlvParser.IPV6_NEIGHBOR_ADDRESS_QNAME, tlv.getIpv6NeighborAddress(), body);
        nlriTypeReg.serializeTlv(MultiTopoIdTlvParser.MULTI_TOPOLOGY_ID_QNAME, tlv.getMultiTopologyId(), body);
    }

    @Override
    public NlriType serializeTypeNlri(final CLinkstateDestination destination, final ByteBuf localdescs, final ByteBuf byteAggregator) {

        final LinkCase lCase = ((LinkCase)destination.getObjectType());
        nlriTypeReg.serializeTlv(LocalNodeDescriptors.QNAME, lCase.getLocalNodeDescriptors(), localdescs);
        TlvUtil.writeTLV(LocalNodeDescriptorTlvC.LOCAL_NODE_DESCRIPTORS_TYPE, localdescs, byteAggregator);
        final ByteBuf rdescs = Unpooled.buffer();
        nlriTypeReg.serializeTlv(RemoteNodeDescriptors.QNAME, lCase.getRemoteNodeDescriptors(), rdescs);
        TlvUtil.writeTLV(LocalNodeDescriptorTlvC.REMOTE_NODE_DESCRIPTORS_TYPE, rdescs, byteAggregator);
        if (lCase.getLinkDescriptors() != null) {
            serializeLinkDescTlvBody(lCase.getLinkDescriptors(), byteAggregator);
        }
        return NlriType.Link;
    }
}
