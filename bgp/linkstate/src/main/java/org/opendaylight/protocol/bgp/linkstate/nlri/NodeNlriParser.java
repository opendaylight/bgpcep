/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.primitives.UnsignedInteger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.epe.rev150622.EpeNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.AreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.OspfInterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.isis.lan.identifier.IsIsRouterIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.node._case.NodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.node._case.NodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.AdvertisingNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.AdvertisingNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.IsisNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.IsisNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.IsisPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.IsisPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.OspfNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.OspfNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.OspfPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.OspfPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.isis.node._case.IsisNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.isis.node._case.IsisNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.ospf.node._case.OspfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.ospf.node._case.OspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;

@VisibleForTesting
public final class NodeNlriParser implements NlriTypeCaseParser, NlriTypeCaseSerializer, NodeDescriptorsTlvBuilderParser {

    public NodeNlriParser() {
    }

    /* Node Descriptor TLVs */
    private static final int AS_NUMBER = 512;
    private static final int BGP_LS_ID = 513;
    private static final int AREA_ID = 514;
    private static final int IGP_ROUTER_ID = 515;
    private static final int BGP_ROUTER_ID = 516;
    private static final int MEMBER_AS_NUMBER = 517;

    /* Node Descriptor Type */
    private static final int LOCAL_NODE_DESCRIPTORS_TYPE = 256;

    /* Node Descriptor QNames */
    @VisibleForTesting
    public static final NodeIdentifier AS_NUMBER_NID = new NodeIdentifier(QName.create(NodeDescriptors.QNAME, "as-number").intern());
    @VisibleForTesting
    public static final NodeIdentifier AREA_NID = new NodeIdentifier(QName.create(NodeDescriptors.QNAME, "area-id").intern());
    @VisibleForTesting
    public static final NodeIdentifier DOMAIN_NID = new NodeIdentifier(QName.create(NodeDescriptors.QNAME, "domain-id").intern());
    @VisibleForTesting
    public static final NodeIdentifier ROUTER_NID = new NodeIdentifier(CRouterIdentifier.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier BGP_ROUTER_NID = new NodeIdentifier(QName.create(NodeDescriptors.QNAME, "bgp-router-id").intern());
    @VisibleForTesting
    public static final NodeIdentifier MEMBER_ASN_NID = new NodeIdentifier(QName.create(NodeDescriptors.QNAME, "member-asn").intern());

    /* Router Identifier QNames */
    @VisibleForTesting
    public static final NodeIdentifier ISIS_NODE_NID = new NodeIdentifier(IsisNode.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier ISIS_PSEUDONODE_NID = new NodeIdentifier(IsisPseudonode.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier OSPF_NODE_NID = new NodeIdentifier(OspfNode.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier OSPF_PSEUDONODE_NID = new NodeIdentifier(OspfPseudonode.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier ISO_SYSTEM_NID = new NodeIdentifier(QName.create(NodeDescriptors.QNAME, "iso-system-id").intern());
    @VisibleForTesting
    public static final NodeIdentifier ISIS_ROUTER_NID = new NodeIdentifier(QName.create(NodeDescriptors.QNAME, "is-is-router-identifier").intern());
    @VisibleForTesting
    public static final NodeIdentifier PSN_NID = new NodeIdentifier(QName.create(NodeDescriptors.QNAME, "psn").intern());
    @VisibleForTesting
    public static final NodeIdentifier OSPF_ROUTER_NID = new NodeIdentifier(QName.create(NodeDescriptors.QNAME, "ospf-router-id").intern());
    private static final NodeIdentifier LAN_IFACE_NID = new NodeIdentifier(QName.create(NodeDescriptors.QNAME, "lan-interface").intern());

    @Override
    public void setAsNumBuilder(AsNumber asNum, NlriTlvTypeBuilderContext context) {
        context.getNodeDescriptorsBuilder().setAsNumber(asNum);
    }

    @Override
    public void setAreaIdBuilder(AreaIdentifier ai, NlriTlvTypeBuilderContext context) {
        context.getNodeDescriptorsBuilder().setAreaId(ai);
    }

    @Override
    public void setCRouterIdBuilder(CRouterIdentifier CRouterId, NlriTlvTypeBuilderContext context) {
        context.getNodeDescriptorsBuilder().setCRouterIdentifier(CRouterId);
    }

    @Override
    public void setDomainIdBuilder(DomainIdentifier bgpId, NlriTlvTypeBuilderContext context) {
        context.getNodeDescriptorsBuilder().setDomainId(bgpId);
    }

    @Override
    public org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier buildNodeDescriptors(NlriTlvTypeBuilderContext context) {
        return context.getNodeDescriptorsBuilder().build();
    }


    static void serializeNodeIdentifier(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier descriptors, final ByteBuf buffer) {
        if (descriptors.getAsNumber() != null) {
            TlvUtil.writeTLV(AS_NUMBER, Unpooled.copyInt(UnsignedInteger.valueOf(descriptors.getAsNumber().getValue()).intValue()), buffer);
        }
        if (descriptors.getDomainId() != null) {
            TlvUtil.writeTLV(BGP_LS_ID, Unpooled.copyInt(UnsignedInteger.valueOf(descriptors.getDomainId().getValue()).intValue()), buffer);
        }
        if (descriptors.getAreaId() != null) {
            TlvUtil.writeTLV(AREA_ID, Unpooled.copyInt(UnsignedInteger.valueOf(descriptors.getAreaId().getValue()).intValue()), buffer);
        }
        if (descriptors.getCRouterIdentifier() != null) {
            final ByteBuf routerIdBuf = Unpooled.buffer();
            serializeRouterId(descriptors.getCRouterIdentifier(), routerIdBuf);
            TlvUtil.writeTLV(IGP_ROUTER_ID, routerIdBuf, buffer);
        }
    }

    static void serializeEpeNodeDescriptors(final EpeNodeDescriptors epends, final ByteBuf buffer) {
        if (epends.getBgpRouterId() != null) {
            TlvUtil.writeTLV(BGP_ROUTER_ID, Ipv4Util.byteBufForAddress(epends.getBgpRouterId()), buffer);
        }
        if (epends.getMemberAsn() != null) {
            TlvUtil.writeTLV(MEMBER_AS_NUMBER, Unpooled.copyInt(UnsignedInteger.valueOf(epends.getMemberAsn().getValue()).intValue()), buffer);
        }
    }

    private static void serializeRouterId(final CRouterIdentifier routerId, final ByteBuf buffer) {
        if (routerId instanceof IsisNodeCase) {
            final IsisNode isis = ((IsisNodeCase) routerId).getIsisNode();
            buffer.writeBytes(isis.getIsoSystemId().getValue());
        } else if (routerId instanceof IsisPseudonodeCase) {
            final IsisPseudonode isis = ((IsisPseudonodeCase) routerId).getIsisPseudonode();
            buffer.writeBytes(isis.getIsIsRouterIdentifier().getIsoSystemId().getValue());
            buffer.writeByte(((isis.getPsn() != null) ? isis.getPsn() : 0));
        } else if (routerId instanceof OspfNodeCase) {
            buffer.writeInt(UnsignedInteger.valueOf(((OspfNodeCase) routerId).getOspfNode().getOspfRouterId()).intValue());
        } else if (routerId instanceof OspfPseudonodeCase) {
            final OspfPseudonode node = ((OspfPseudonodeCase) routerId).getOspfPseudonode();
            buffer.writeInt(UnsignedInteger.valueOf(node.getOspfRouterId()).intValue());
            buffer.writeInt(UnsignedInteger.valueOf(node.getLanInterface().getValue()).intValue());
        }
    }

    private static IsisNodeCase serializeIsisNode(final ContainerNode isis) {
        final IsisNodeCaseBuilder builder = new IsisNodeCaseBuilder();
        final IsisNodeBuilder isisBuilder = new IsisNodeBuilder();
        isisBuilder.setIsoSystemId(new IsoSystemIdentifier((byte[]) isis.getChild(ISO_SYSTEM_NID).get().getValue()));
        builder.setIsisNode(isisBuilder.build());
        return builder.build();
    }

    private static IsisPseudonodeCase serializeIsisPseudoNode(final ContainerNode pseudoIsisNode) {
        final IsIsRouterIdentifierBuilder isisRouterId = new IsIsRouterIdentifierBuilder();
        if (pseudoIsisNode.getChild(ISIS_ROUTER_NID).isPresent()) {
            final ContainerNode isisRouterNid = (ContainerNode) pseudoIsisNode.getChild(ISIS_ROUTER_NID).get();
            if (isisRouterNid.getChild(ISO_SYSTEM_NID).isPresent()) {
                isisRouterId.setIsoSystemId(new IsoSystemIdentifier((byte[]) isisRouterNid.getChild(ISO_SYSTEM_NID).get().getValue()));
            }
        }

        final IsisPseudonodeBuilder nodeBuilder = new IsisPseudonodeBuilder();
        nodeBuilder.setIsIsRouterIdentifier(isisRouterId.build());

        if (pseudoIsisNode.getChild(PSN_NID).isPresent()) {
            nodeBuilder.setPsn((Short) pseudoIsisNode.getChild(PSN_NID).get().getValue());
        } else {
            nodeBuilder.setPsn((short) 0);
        }

        return new IsisPseudonodeCaseBuilder().setIsisPseudonode(nodeBuilder.build()).build();
    }

    private static OspfNodeCase serializeOspfNode(final ContainerNode ospf) {
        final OspfNodeCaseBuilder builder = new OspfNodeCaseBuilder();
        if (ospf.getChild(OSPF_ROUTER_NID).isPresent()) {
            final OspfNodeBuilder nodeBuilder = new OspfNodeBuilder();
            nodeBuilder.setOspfRouterId((Long) ospf.getChild(OSPF_ROUTER_NID).get().getValue());
            builder.setOspfNode(nodeBuilder.build());
        }
        return builder.build();
    }

    private static CRouterIdentifier serializeOspfPseudoNode(final ContainerNode ospfPseudonode) {
        final OspfPseudonodeCaseBuilder builder = new OspfPseudonodeCaseBuilder();
        final OspfPseudonodeBuilder nodeBuilder = new OspfPseudonodeBuilder();
        if (ospfPseudonode.getChild(LAN_IFACE_NID).isPresent()) {
            nodeBuilder.setLanInterface(new OspfInterfaceIdentifier((Long)ospfPseudonode.getChild(LAN_IFACE_NID).get().getValue()));
        }
        if (ospfPseudonode.getChild(OSPF_ROUTER_NID).isPresent()) {
            nodeBuilder.setOspfRouterId((Long)ospfPseudonode.getChild(OSPF_ROUTER_NID).get().getValue());
        }
        builder.setOspfPseudonode(nodeBuilder.build());
        return builder.build();
    }

    private static CRouterIdentifier serializeRouterId(final ContainerNode descriptorsData) {
        CRouterIdentifier cRouterId = null;
        final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRouterId = descriptorsData.getChild(ROUTER_NID);
        if (maybeRouterId.isPresent()) {
            final ChoiceNode routerId = (ChoiceNode) maybeRouterId.get();
            if (routerId.getChild(ISIS_NODE_NID).isPresent()) {
                cRouterId = serializeIsisNode((ContainerNode) routerId.getChild(ISIS_NODE_NID).get());
            } else if (routerId.getChild(ISIS_PSEUDONODE_NID).isPresent()) {
                cRouterId = serializeIsisPseudoNode((ContainerNode) routerId.getChild(ISIS_PSEUDONODE_NID).get());
            } else if (routerId.getChild(OSPF_NODE_NID).isPresent()) {
                cRouterId = serializeOspfNode((ContainerNode) routerId.getChild(OSPF_NODE_NID).get());
            } else if (routerId.getChild(OSPF_PSEUDONODE_NID).isPresent()) {
                cRouterId = serializeOspfPseudoNode((ContainerNode) routerId.getChild(OSPF_PSEUDONODE_NID).get());
            }
        }
        return cRouterId;
    }

    private static AsNumber serializeAsNumber(final ContainerNode descriptorsData) {
        final Optional<DataContainerChild<? extends PathArgument, ?>> asNumber = descriptorsData.getChild(AS_NUMBER_NID);
        if (asNumber.isPresent()) {
            return new AsNumber((Long) asNumber.get().getValue());
        }
        return null;
    }

    private static DomainIdentifier serializeDomainId(final ContainerNode descriptorsData) {
        final Optional<DataContainerChild<? extends PathArgument, ?>> domainId = descriptorsData.getChild(DOMAIN_NID);
        if (domainId.isPresent()) {
            return new DomainIdentifier((Long) domainId.get().getValue());
        }
        return null;
    }

    private static AreaIdentifier serializeAreaId(final ContainerNode descriptorsData) {
        final Optional<DataContainerChild<? extends PathArgument, ?>> areaId = descriptorsData.getChild(AREA_NID);
        if (areaId.isPresent()) {
            return new AreaIdentifier((Long) areaId.get().getValue());
        }
        return null;
    }

    private static Ipv4Address serializeBgpRouterId(final ContainerNode descriptorsData) {
        final Optional<DataContainerChild<? extends PathArgument, ?>> bgpRouterId = descriptorsData.getChild(BGP_ROUTER_NID);
        if (bgpRouterId.isPresent()) {
            return new Ipv4Address((String) bgpRouterId.get().getValue());
        }
        return null;
    }

    private static AsNumber serializeMemberAsn(final ContainerNode descriptorsData) {
        final Optional<DataContainerChild<? extends PathArgument, ?>> memberAsn = descriptorsData.getChild(MEMBER_ASN_NID);
        if (memberAsn.isPresent()) {
            return new AsNumber((Long) memberAsn.get().getValue());
        }
        return null;
    }

    static LocalNodeDescriptors serializeLocalNodeDescriptors(final ContainerNode descriptorsData) {
        final LocalNodeDescriptorsBuilder builder = new LocalNodeDescriptorsBuilder();
        builder.setAsNumber(serializeAsNumber(descriptorsData));
        builder.setDomainId(serializeDomainId(descriptorsData));
        builder.setAreaId(serializeAreaId(descriptorsData));
        builder.setCRouterIdentifier(serializeRouterId(descriptorsData));
        builder.setBgpRouterId(serializeBgpRouterId(descriptorsData));
        builder.setMemberAsn(serializeMemberAsn(descriptorsData));
        return builder.build();
    }

    static RemoteNodeDescriptors serializeRemoteNodeDescriptors(final ContainerNode descriptorsData) {
        final RemoteNodeDescriptorsBuilder builder = new RemoteNodeDescriptorsBuilder();
        builder.setAsNumber(serializeAsNumber(descriptorsData));
        builder.setDomainId(serializeDomainId(descriptorsData));
        builder.setAreaId(serializeAreaId(descriptorsData));
        builder.setCRouterIdentifier(serializeRouterId(descriptorsData));
        builder.setBgpRouterId(serializeBgpRouterId(descriptorsData));
        builder.setMemberAsn(serializeMemberAsn(descriptorsData));
        return builder.build();
    }

    static AdvertisingNodeDescriptors serializeAdvNodeDescriptors(final ContainerNode descriptorsData) {
        final AdvertisingNodeDescriptorsBuilder builder = new AdvertisingNodeDescriptorsBuilder();
        builder.setAsNumber(serializeAsNumber(descriptorsData));
        builder.setDomainId(serializeDomainId(descriptorsData));
        builder.setAreaId(serializeAreaId(descriptorsData));
        builder.setCRouterIdentifier(serializeRouterId(descriptorsData));
        return builder.build();
    }

    static NodeDescriptors serializeNodeDescriptors(final ContainerNode descriptorsData) {
        final NodeDescriptorsBuilder builder = new NodeDescriptorsBuilder();
        builder.setAsNumber(serializeAsNumber(descriptorsData));
        builder.setDomainId(serializeDomainId(descriptorsData));
        builder.setAreaId(serializeAreaId(descriptorsData));
        builder.setCRouterIdentifier(serializeRouterId(descriptorsData));
        return builder.build();
    }

    @Override
    public ObjectType parseTypeNlri(final ByteBuf nlri, final NlriType type, final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier localdescriptor, final ByteBuf restNlri) throws BGPParsingException {

        NodeCaseBuilder nodebuilder = new NodeCaseBuilder();
        NodeCase nodecase = nodebuilder.setNodeDescriptors((NodeDescriptors) localdescriptor).build();
        return nodecase;

    }

    @Override
    public NlriType serializeTypeNlri(final CLinkstateDestination destination, final ByteBuf localdescs, final ByteBuf byteAggregator) {
        final ObjectType nCase = destination.getObjectType();
        SimpleNlriTypeRegistry.getInstance().serializeNodeTlvObject(nCase, NlriType.Node, localdescs);
        TlvUtil.writeTLV(LOCAL_NODE_DESCRIPTORS_TYPE, localdescs, byteAggregator);
        return NlriType.Node;
    }

}
