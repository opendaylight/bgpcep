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
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Ipv6InterfaceIdentifier;
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
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

@VisibleForTesting
public final class LinkNlriParser implements NlriTypeCaseParser, NlriTypeCaseSerializer {

    /* Node Descriptor Type */
    private static final int LOCAL_NODE_DESCRIPTORS_TYPE = 256;
    private static final int REMOTE_NODE_DESCRIPTORS_TYPE = 257;

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

    @Override
    public ObjectType parseTypeNlri(final ByteBuf nlri, final NlriType type, final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier localDescriptor, final ByteBuf restNlri) throws BGPParsingException {

        final int nodetype = restNlri.readUnsignedShort();
        final int length = restNlri.readUnsignedShort();
        final SimpleNlriTypeRegistry nlriTypeReg = SimpleNlriTypeRegistry.getInstance();
        RemoteNodeDescriptors remoteDescriptors = null;
        if (nodetype == REMOTE_NODE_DESCRIPTORS_TYPE) {
            remoteDescriptors = (RemoteNodeDescriptors) nlriTypeReg.parseTlvObject(restNlri.readSlice(length), type, LinkstateNlriParser.REMOTE_NODE_DESCRIPTORS_NID);
        }
        LinkDescriptors linkdescriptor = (LinkDescriptors) nlriTypeReg.parseTlvObject(restNlri.slice(), type, LinkstateNlriParser.LINK_DESCRIPTORS_NID);
        LinkCaseBuilder linkbuilder = new LinkCaseBuilder();
        LinkCase linkcase = linkbuilder.setLocalNodeDescriptors((LocalNodeDescriptors) localDescriptor).setRemoteNodeDescriptors(remoteDescriptors).setLinkDescriptors(linkdescriptor).build();
        return linkcase;
    }

    @Override
    public NlriType serializeTypeNlri(final CLinkstateDestination destination, final ByteBuf localdescs, final ByteBuf byteAggregator) {

        final ObjectType lCase = destination.getObjectType();
        final SimpleNlriTypeRegistry nlriTypeReg = SimpleNlriTypeRegistry.getInstance();
        nlriTypeReg.serializeTlvObject(lCase, LinkstateNlriParser.LOCAL_NODE_DESCRIPTORS_NID, NlriType.Link, localdescs);
        TlvUtil.writeTLV(LOCAL_NODE_DESCRIPTORS_TYPE, localdescs, byteAggregator);
        final ByteBuf rdescs = Unpooled.buffer();
        nlriTypeReg.serializeTlvObject(lCase, LinkstateNlriParser.REMOTE_NODE_DESCRIPTORS_NID, NlriType.Link, rdescs);
        TlvUtil.writeTLV(REMOTE_NODE_DESCRIPTORS_TYPE, rdescs, byteAggregator);
        if (((LinkCase)lCase).getLinkDescriptors() != null) {
            nlriTypeReg.serializeTlvObject(lCase, LinkstateNlriParser.LINK_DESCRIPTORS_NID, NlriType.Link, byteAggregator);
        }
        return NlriType.Link;
    }
}
