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
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.AreaIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.AsNumTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.BgpRouterIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.DomainIdTlvParser;
import org.opendaylight.protocol.bgp.linkstate.impl.tlvs.MemAsNumTlvParser;
import org.opendaylight.protocol.bgp.linkstate.spi.AbstractNlriTypeCodec;
import org.opendaylight.protocol.bgp.linkstate.spi.pojo.SimpleNlriTypeRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.AreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.OspfInterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.isis.lan.identifier.IsIsRouterIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.NodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.link._case.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.node._case.NodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.node._case.NodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.prefix._case.AdvertisingNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.prefix._case.AdvertisingNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.IsisNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.IsisNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.IsisPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.IsisPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.OspfNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.OspfNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.OspfPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.isis.node._case.IsisNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.isis.node._case.IsisNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.ospf.node._case.OspfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.ospf.node._case.OspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;

public final class NodeNlriParser extends AbstractNlriTypeCodec {

    /* Node Descriptor QNames */
    @VisibleForTesting
    public static final NodeIdentifier AS_NUMBER_NID = NodeIdentifier.create(AsNumTlvParser.AS_NUMBER_QNAME);
    @VisibleForTesting
    public static final NodeIdentifier AREA_NID = NodeIdentifier.create(AreaIdTlvParser.AREA_ID_QNAME);
    @VisibleForTesting
    public static final NodeIdentifier DOMAIN_NID = NodeIdentifier.create(DomainIdTlvParser.DOMAIN_ID_QNAME);
    @VisibleForTesting
    private static final NodeIdentifier ROUTER_NID = NodeIdentifier.create(CRouterIdentifier.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier BGP_ROUTER_NID = NodeIdentifier.create(BgpRouterIdTlvParser.BGP_ROUTER_ID_QNAME);
    @VisibleForTesting
    public static final NodeIdentifier MEMBER_ASN_NID = NodeIdentifier.create(MemAsNumTlvParser.MEMBER_AS_NUMBER_QNAME);

    /* Router Identifier QNames */
    @VisibleForTesting
    public static final NodeIdentifier ISIS_NODE_NID = NodeIdentifier.create(IsisNode.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier ISIS_PSEUDONODE_NID = NodeIdentifier.create(IsisPseudonode.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier OSPF_NODE_NID = NodeIdentifier.create(OspfNode.QNAME);
    @VisibleForTesting
    private static final NodeIdentifier OSPF_PSEUDONODE_NID = NodeIdentifier.create(OspfPseudonode.QNAME);
    @VisibleForTesting
    public static final NodeIdentifier ISO_SYSTEM_NID = NodeIdentifier.create(
            QName.create(NodeDescriptors.QNAME, "iso-system-id"));
    @VisibleForTesting
    public static final NodeIdentifier ISIS_ROUTER_NID = NodeIdentifier.create(
            QName.create(NodeDescriptors.QNAME, "is-is-router-identifier").intern());
    @VisibleForTesting
    public static final NodeIdentifier PSN_NID = NodeIdentifier.create(
            QName.create(NodeDescriptors.QNAME, "psn").intern());
    @VisibleForTesting
    public static final NodeIdentifier OSPF_ROUTER_NID = NodeIdentifier.create(
            QName.create(NodeDescriptors.QNAME, "ospf-router-id").intern());
    private static final NodeIdentifier LAN_IFACE_NID = NodeIdentifier.create(
            QName.create(NodeDescriptors.QNAME, "lan-interface").intern());

    @Override
    protected ObjectType parseObjectType(final ByteBuf buffer) {
        final NodeCaseBuilder builder = new NodeCaseBuilder();
        builder.setNodeDescriptors(new NodeDescriptorsBuilder((org.opendaylight.yang.gen.v1.urn.opendaylight.params
                .xml.ns.yang.bgp.linkstate.rev200120.NodeIdentifier) SimpleNlriTypeRegistry.getInstance()
                .parseTlv(buffer)).build());
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
        return new IsisNodeCaseBuilder()
            .setIsisNode(new IsisNodeBuilder()
                .setIsoSystemId(new IsoSystemIdentifier((byte[]) isis.findChildByArg(ISO_SYSTEM_NID).get().body()))
                .build())
            .build();
    }

    private static IsisPseudonodeCase serializeIsisPseudoNode(final ContainerNode pseudoIsisNode) {
        final IsIsRouterIdentifierBuilder isisRouterId = new IsIsRouterIdentifierBuilder();
        if (pseudoIsisNode.findChildByArg(ISIS_ROUTER_NID).isPresent()) {
            final ContainerNode isisRouterNid = (ContainerNode) pseudoIsisNode.findChildByArg(ISIS_ROUTER_NID).get();
            if (isisRouterNid.findChildByArg(ISO_SYSTEM_NID).isPresent()) {
                isisRouterId.setIsoSystemId(
                    new IsoSystemIdentifier((byte[]) isisRouterNid.findChildByArg(ISO_SYSTEM_NID).get().body()));
            }
        }

        final IsisPseudonodeBuilder nodeBuilder = new IsisPseudonodeBuilder();
        nodeBuilder.setIsIsRouterIdentifier(isisRouterId.build());

        if (pseudoIsisNode.findChildByArg(PSN_NID).isPresent()) {
            nodeBuilder.setPsn((Uint8) pseudoIsisNode.findChildByArg(PSN_NID).get().body());
        } else {
            nodeBuilder.setPsn(Uint8.ZERO);
        }

        return new IsisPseudonodeCaseBuilder().setIsisPseudonode(nodeBuilder.build()).build();
    }

    private static OspfNodeCase serializeOspfNode(final ContainerNode ospf) {
        final OspfNodeCaseBuilder builder = new OspfNodeCaseBuilder();
        ospf.findChildByArg(OSPF_ROUTER_NID)
            .map(routerId -> new OspfNodeBuilder().setOspfRouterId((Uint32) routerId.body()).build())
            .ifPresent(builder::setOspfNode);
        return builder.build();
    }

    private static CRouterIdentifier serializeOspfPseudoNode(final ContainerNode ospfPseudonode) {
        final OspfPseudonodeBuilder nodeBuilder = new OspfPseudonodeBuilder();

        ospfPseudonode.findChildByArg(LAN_IFACE_NID)
            .map(lanIface -> new OspfInterfaceIdentifier((Uint32) lanIface.body()))
            .ifPresent(nodeBuilder::setLanInterface);
        ospfPseudonode.findChildByArg(OSPF_ROUTER_NID)
            .map(ospfRouter -> (Uint32) ospfRouter.body())
            .ifPresent(nodeBuilder::setOspfRouterId);

        return new OspfPseudonodeCaseBuilder()
            .setOspfPseudonode(nodeBuilder.build())
            .build();
    }

    private static CRouterIdentifier serializeRouterId(final ContainerNode descriptorsData) {
        final ChoiceNode routerId = (ChoiceNode) descriptorsData.childByArg(ROUTER_NID);
        if (routerId != null) {
            DataContainerChild nid = routerId.childByArg(ISIS_NODE_NID);
            if (nid != null) {
                return serializeIsisNode((ContainerNode) nid);
            }
            nid = routerId.childByArg(ISIS_PSEUDONODE_NID);
            if (nid != null) {
                return serializeIsisPseudoNode((ContainerNode) nid);
            }
            nid = routerId.childByArg(OSPF_NODE_NID);
            if (nid != null) {
                return serializeOspfNode((ContainerNode) nid);
            }
            nid = routerId.childByArg(OSPF_PSEUDONODE_NID);
            if (nid != null) {
                return serializeOspfPseudoNode((ContainerNode) nid);
            }
        }
        return null;
    }

    private static AsNumber serializeAsNumber(final ContainerNode descriptorsData) {
        return descriptorsData.findChildByArg(AS_NUMBER_NID).map(
            dataContainerChild -> new AsNumber((Uint32) dataContainerChild.body())).orElse(null);
    }

    private static DomainIdentifier serializeDomainId(final ContainerNode descriptorsData) {
        return descriptorsData.findChildByArg(DOMAIN_NID).map(
            dataContainerChild -> new DomainIdentifier((Uint32) dataContainerChild.body())).orElse(null);
    }

    private static AreaIdentifier serializeAreaId(final ContainerNode descriptorsData) {
        return descriptorsData.findChildByArg(AREA_NID).map(
            dataContainerChild -> new AreaIdentifier((Uint32) dataContainerChild.body())).orElse(null);
    }

    private static Ipv4AddressNoZone serializeBgpRouterId(final ContainerNode descriptorsData) {
        return descriptorsData.findChildByArg(BGP_ROUTER_NID).map(
            dataContainerChild -> new Ipv4AddressNoZone((String) dataContainerChild.body())).orElse(null);
    }

    private static AsNumber serializeMemberAsn(final ContainerNode descriptorsData) {
        return descriptorsData.findChildByArg(MEMBER_ASN_NID).map(
            dataContainerChild -> new AsNumber((Uint32) dataContainerChild.body())).orElse(null);
    }

    static LocalNodeDescriptors serializeLocalNodeDescriptors(final ContainerNode descriptorsData) {
        return new LocalNodeDescriptorsBuilder()
                .setAsNumber(serializeAsNumber(descriptorsData))
                .setDomainId(serializeDomainId(descriptorsData))
                .setAreaId(serializeAreaId(descriptorsData))
                .setCRouterIdentifier(serializeRouterId(descriptorsData))
                .setBgpRouterId(serializeBgpRouterId(descriptorsData))
                .setMemberAsn(serializeMemberAsn(descriptorsData))
                .build();
    }

    static RemoteNodeDescriptors serializeRemoteNodeDescriptors(final ContainerNode descriptorsData) {
        return new RemoteNodeDescriptorsBuilder()
                .setAsNumber(serializeAsNumber(descriptorsData))
                .setDomainId(serializeDomainId(descriptorsData))
                .setAreaId(serializeAreaId(descriptorsData))
                .setCRouterIdentifier(serializeRouterId(descriptorsData))
                .setBgpRouterId(serializeBgpRouterId(descriptorsData))
                .setMemberAsn(serializeMemberAsn(descriptorsData))
                .build();
    }

    static AdvertisingNodeDescriptors serializeAdvNodeDescriptors(final ContainerNode descriptorsData) {
        return new AdvertisingNodeDescriptorsBuilder()
                .setAsNumber(serializeAsNumber(descriptorsData))
                .setDomainId(serializeDomainId(descriptorsData))
                .setAreaId(serializeAreaId(descriptorsData))
                .setCRouterIdentifier(serializeRouterId(descriptorsData))
                .build();
    }

    static NodeDescriptors serializeNodeDescriptors(final ContainerNode descriptorsData) {
        return new NodeDescriptorsBuilder()
                .setAsNumber(serializeAsNumber(descriptorsData))
                .setDomainId(serializeDomainId(descriptorsData))
                .setAreaId(serializeAreaId(descriptorsData))
                .setCRouterIdentifier(serializeRouterId(descriptorsData))
                .build();
    }
}
