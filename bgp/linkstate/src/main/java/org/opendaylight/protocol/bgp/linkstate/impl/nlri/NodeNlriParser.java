/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.nlri;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.AreaIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.AsNumTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.BgpRouterIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.DomainIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.MemAsNumTlvParser;
import org.opendaylight.protocol.bgp.linkstate.spi.AbstractNlriTypeCodec;
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleNlriTypeRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.AreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.OspfInterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.isis.lan.identifier.IsIsRouterIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.NodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.node._case.NodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.node._case.NodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.prefix._case.AdvertisingNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.prefix._case.AdvertisingNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.IsisNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.IsisNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.IsisPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.IsisPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.OspfNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.OspfNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.OspfPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.isis.node._case.IsisNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.isis.node._case.IsisNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.ospf.node._case.OspfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.ospf.node._case.OspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;

public final class NodeNlriParser extends AbstractNlriTypeCodec {

    /* Node Descriptor QNames */
    @VisibleForTesting
    public static final NodeIdentifier AS_NUMBER_NID = new NodeIdentifier(AsNumTlvParser.AS_NUMBER_QNAME);
    @VisibleForTesting
    public static final NodeIdentifier AREA_NID = new NodeIdentifier(AreaIdTlvParser.AREA_ID_QNAME);
    @VisibleForTesting
    public static final NodeIdentifier DOMAIN_NID = new NodeIdentifier(DomainIdTlvParser.DOMAIN_ID_QNAME);
    @VisibleForTesting
    private static final NodeIdentifier ROUTER_NID = new NodeIdentifier(CRouterIdentifier.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier BGP_ROUTER_NID = new NodeIdentifier(BgpRouterIdTlvParser.BGP_ROUTER_ID_QNAME);
    @VisibleForTesting
    public static final NodeIdentifier MEMBER_ASN_NID = new NodeIdentifier(MemAsNumTlvParser.MEMBER_AS_NUMBER_QNAME);

    /* Router Identifier QNames */
    @VisibleForTesting
    public static final NodeIdentifier ISIS_NODE_NID = new NodeIdentifier(IsisNode.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier ISIS_PSEUDONODE_NID = new NodeIdentifier(IsisPseudonode.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier OSPF_NODE_NID = new NodeIdentifier(OspfNode.QNAME);
    @VisibleForTesting
    private static final NodeIdentifier OSPF_PSEUDONODE_NID = new NodeIdentifier(OspfPseudonode.QNAME);
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
    protected ObjectType parseObjectType(final ByteBuf buffer) {
        final NodeCaseBuilder builder = new NodeCaseBuilder();
        builder.setNodeDescriptors(new NodeDescriptorsBuilder((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.NodeIdentifier) SimpleNlriTypeRegistry.getInstance().parseTlv(buffer)).build());
        return builder.build();
    }

    @Override
    protected void serializeObjectType(final ObjectType objectType, final ByteBuf buffer) {
        final NodeCase node = (NodeCase) objectType;
        SimpleNlriTypeRegistry.getInstance().serializeTlv(NodeDescriptors.QNAME, node.getNodeDescriptors(), buffer);
    }

    @Override
    public int getNlriType() {
        return NlriType.Node.getIntValue();
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
}
