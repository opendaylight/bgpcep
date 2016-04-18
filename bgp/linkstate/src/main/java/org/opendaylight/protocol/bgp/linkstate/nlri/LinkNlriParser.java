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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.AreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.DomainIdentifier;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.CRouterIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

@VisibleForTesting
public final class LinkNlriParser implements NlriTypeCaseParser, NlriTypeCaseSerializer, NodeDescriptorsTlvBuilderParser {

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
    public void setAsNumBuilder(final AsNumber asNum, final NlriTlvTypeBuilderContext context) {
        if (context.isLocal()) {
            context.getLocalNodeDescBuilder().setAsNumber(asNum);
        } else {
            context.getRemoteNodeDescBuilder().setAsNumber(asNum);
        }
    }

    @Override
    public void setAreaIdBuilder(AreaIdentifier ai, NlriTlvTypeBuilderContext context) {
        if (context.isLocal()) {
            context.getLocalNodeDescBuilder().setAreaId(ai);
        } else {
            context.getRemoteNodeDescBuilder().setAreaId(ai);
        }
    }

    public void setBgpRidBuilder(Ipv4Address rid, NlriTlvTypeBuilderContext context) {
        if (context.isLocal()) {
            context.getLocalNodeDescBuilder().setBgpRouterId(rid);
        } else {
            context.getRemoteNodeDescBuilder().setBgpRouterId(rid);
        }
    }

    @Override
    public void setCRouterIdBuilder(CRouterIdentifier CRouterId, NlriTlvTypeBuilderContext context) {
        if (context.isLocal()) {
            context.getLocalNodeDescBuilder().setCRouterIdentifier(CRouterId);
        } else {
            context.getRemoteNodeDescBuilder().setCRouterIdentifier(CRouterId);
        }
    }

    @Override
    public void setDomainIdBuilder(DomainIdentifier bgpId, NlriTlvTypeBuilderContext context) {
        if (context.isLocal()) {
            context.getLocalNodeDescBuilder().setDomainId(bgpId);
        } else {
            context.getRemoteNodeDescBuilder().setDomainId(bgpId);
        }
    }

    public void setMemAsNumBuilder(AsNumber memberAsn, NlriTlvTypeBuilderContext context) {
        if (context.isLocal()) {
            context.getLocalNodeDescBuilder().setMemberAsn(memberAsn);
        } else {
            context.getRemoteNodeDescBuilder().setMemberAsn(memberAsn);
        }
    }

    @Override
    public org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier buildNodeDescriptors(NlriTlvTypeBuilderContext context) {
        if (context.isLocal()) {
            return context.getLocalNodeDescBuilder().build();
        } else {
            return context.getRemoteNodeDescBuilder().build();
        }
    }


    @Override
    public ObjectType parseTypeNlri(final ByteBuf nlri, final NlriType type, final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier localdescriptor, final ByteBuf restNlri) throws BGPParsingException {

        final int nodetype = restNlri.readUnsignedShort();
        final int length = restNlri.readUnsignedShort();
        final SimpleNlriTypeRegistry nlriTypeReg = SimpleNlriTypeRegistry.getInstance();
        RemoteNodeDescriptors remoteDescriptors = null;
        final NlriTlvTypeBuilderContext context = new NlriTlvTypeBuilderContext();
        if (nodetype == REMOTE_NODE_DESCRIPTORS_TYPE) {
            nlriTypeReg.parseTlvObject(restNlri.readSlice(length), type, context);
            remoteDescriptors = (RemoteNodeDescriptors) nlriTypeReg.nodeDescriptorTlvBuilder(context, type);
        }
        nlriTypeReg.parseTlvObject(restNlri.slice(), type, context);
        LinkDescriptors linkdescriptor = context.getLinkDescriptorsBuilder().build();
        LinkCaseBuilder linkbuilder = new LinkCaseBuilder();
        LinkCase linkcase = linkbuilder.setLocalNodeDescriptors((LocalNodeDescriptors) localdescriptor).setRemoteNodeDescriptors(remoteDescriptors).setLinkDescriptors(linkdescriptor).build();
        return linkcase;
    }

    @Override
    public NlriType serializeTypeNlri(final CLinkstateDestination destination, final ByteBuf localdescs, final ByteBuf byteAggregator) {

        final ObjectType lCase = destination.getObjectType();
        final SimpleNlriTypeRegistry nlriTypeReg = SimpleNlriTypeRegistry.getInstance();
        nlriTypeReg.serializeNodeTlvObject(lCase, NlriType.Link, localdescs);
        nlriTypeReg.serializeEpeNodeTlvObject(lCase, NlriType.Link, localdescs, true);
        TlvUtil.writeTLV(LOCAL_NODE_DESCRIPTORS_TYPE, localdescs, byteAggregator);
        final ByteBuf rdescs = Unpooled.buffer();
        nlriTypeReg.serializeRemNodeTlvObject(lCase, NlriType.Link, rdescs);
        nlriTypeReg.serializeEpeNodeTlvObject(lCase, NlriType.Link, rdescs, false);
        TlvUtil.writeTLV(REMOTE_NODE_DESCRIPTORS_TYPE, rdescs, byteAggregator);
        if (((LinkCase)lCase).getLinkDescriptors() != null) {
            nlriTypeReg.serializeLinkTlvObject(lCase, NlriType.Link, byteAggregator);
        }
        return NlriType.Link;
    }
}
