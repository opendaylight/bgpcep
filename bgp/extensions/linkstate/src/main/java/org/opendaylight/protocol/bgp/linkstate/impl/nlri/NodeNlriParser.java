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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.AreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.OspfInterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.isis.lan.identifier.IsIsRouterIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.object.type.NodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.object.type.link._case.LocalNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.object.type.link._case.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.object.type.link._case.RemoteNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.object.type.link._case.RemoteNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.object.type.node._case.NodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.object.type.node._case.NodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.object.type.prefix._case.AdvertisingNodeDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.linkstate.object.type.prefix._case.AdvertisingNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.node.identifier.c.router.identifier.IsisNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.node.identifier.c.router.identifier.IsisNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.node.identifier.c.router.identifier.IsisPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.node.identifier.c.router.identifier.IsisPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.node.identifier.c.router.identifier.OspfNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.node.identifier.c.router.identifier.OspfNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.node.identifier.c.router.identifier.OspfPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.node.identifier.c.router.identifier.isis.node._case.IsisNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.node.identifier.c.router.identifier.isis.node._case.IsisNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.node.identifier.c.router.identifier.ospf.node._case.OspfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.node.identifier.c.router.identifier.ospf.node._case.OspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
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
                .xml.ns.yang.bgp.linkstate.rev180329.NodeIdentifier) SimpleNlriTypeRegistry.getInstance()
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
                .setIsisNode(new IsisNodeBuilder().setIsoSystemId(
                    new IsoSystemIdentifier((byte[]) isis.getChild(ISO_SYSTEM_NID).get().getValue())).build())
                .build();
    }

    private static IsisPseudonodeCase serializeIsisPseudoNode(final ContainerNode pseudoIsisNode) {
        final IsIsRouterIdentifierBuilder isisRouterId = new IsIsRouterIdentifierBuilder();
        if (pseudoIsisNode.getChild(ISIS_ROUTER_NID).isPresent()) {
            final ContainerNode isisRouterNid = (ContainerNode) pseudoIsisNode.getChild(ISIS_ROUTER_NID).get();
            if (isisRouterNid.getChild(ISO_SYSTEM_NID).isPresent()) {
                isisRouterId.setIsoSystemId(new IsoSystemIdentifier((byte[]) isisRouterNid.getChild(ISO_SYSTEM_NID)
                        .get().getValue()));
            }
        }

        final IsisPseudonodeBuilder nodeBuilder = new IsisPseudonodeBuilder();
        nodeBuilder.setIsIsRouterIdentifier(isisRouterId.build());

        if (pseudoIsisNode.getChild(PSN_NID).isPresent()) {
            nodeBuilder.setPsn((Uint8) pseudoIsisNode.getChild(PSN_NID).get().getValue());
        } else {
            nodeBuilder.setPsn(Uint8.ZERO);
        }

        return new IsisPseudonodeCaseBuilder().setIsisPseudonode(nodeBuilder.build()).build();
    }

    private static OspfNodeCase serializeOspfNode(final ContainerNode ospf) {
        final OspfNodeCaseBuilder builder = new OspfNodeCaseBuilder();
        if (ospf.getChild(OSPF_ROUTER_NID).isPresent()) {
            final OspfNodeBuilder nodeBuilder = new OspfNodeBuilder();
            nodeBuilder.setOspfRouterId((Uint32) ospf.getChild(OSPF_ROUTER_NID).get().getValue());
            builder.setOspfNode(nodeBuilder.build());
        }
        return builder.build();
    }

    private static CRouterIdentifier serializeOspfPseudoNode(final ContainerNode ospfPseudonode) {
        final OspfPseudonodeCaseBuilder builder = new OspfPseudonodeCaseBuilder();
        final OspfPseudonodeBuilder nodeBuilder = new OspfPseudonodeBuilder();
        if (ospfPseudonode.getChild(LAN_IFACE_NID).isPresent()) {
            nodeBuilder.setLanInterface(new OspfInterfaceIdentifier((Uint32)ospfPseudonode.getChild(LAN_IFACE_NID)
                    .get().getValue()));
        }
        if (ospfPseudonode.getChild(OSPF_ROUTER_NID).isPresent()) {
            nodeBuilder.setOspfRouterId((Uint32)ospfPseudonode.getChild(OSPF_ROUTER_NID).get().getValue());
        }
        builder.setOspfPseudonode(nodeBuilder.build());
        return builder.build();
    }

    private static CRouterIdentifier serializeRouterId(final ContainerNode descriptorsData) {
        CRouterIdentifier ret = null;
        final Optional<DataContainerChild<? extends PathArgument, ?>> maybeRouterId =
                descriptorsData.getChild(ROUTER_NID);
        if (maybeRouterId.isPresent()) {
            final ChoiceNode routerId = (ChoiceNode) maybeRouterId.get();
            if (routerId.getChild(ISIS_NODE_NID).isPresent()) {
                ret = serializeIsisNode((ContainerNode) routerId.getChild(ISIS_NODE_NID).get());
            } else if (routerId.getChild(ISIS_PSEUDONODE_NID).isPresent()) {
                ret = serializeIsisPseudoNode((ContainerNode) routerId.getChild(ISIS_PSEUDONODE_NID).get());
            } else if (routerId.getChild(OSPF_NODE_NID).isPresent()) {
                ret = serializeOspfNode((ContainerNode) routerId.getChild(OSPF_NODE_NID).get());
            } else if (routerId.getChild(OSPF_PSEUDONODE_NID).isPresent()) {
                ret = serializeOspfPseudoNode((ContainerNode) routerId.getChild(OSPF_PSEUDONODE_NID).get());
            }
        }
        return ret;
    }

    private static AsNumber serializeAsNumber(final ContainerNode descriptorsData) {
        return descriptorsData.getChild(AS_NUMBER_NID).map(
            dataContainerChild -> new AsNumber((Uint32) dataContainerChild.getValue())).orElse(null);
    }

    private static DomainIdentifier serializeDomainId(final ContainerNode descriptorsData) {
        return descriptorsData.getChild(DOMAIN_NID).map(
            dataContainerChild -> new DomainIdentifier((Uint32) dataContainerChild.getValue())).orElse(null);
    }

    private static AreaIdentifier serializeAreaId(final ContainerNode descriptorsData) {
        return descriptorsData.getChild(AREA_NID).map(
            dataContainerChild -> new AreaIdentifier((Uint32) dataContainerChild.getValue())).orElse(null);
    }

    private static Ipv4AddressNoZone serializeBgpRouterId(final ContainerNode descriptorsData) {
        return descriptorsData.getChild(BGP_ROUTER_NID).map(
            dataContainerChild -> new Ipv4AddressNoZone((String) dataContainerChild.getValue())).orElse(null);
    }

    private static AsNumber serializeMemberAsn(final ContainerNode descriptorsData) {
        return descriptorsData.getChild(MEMBER_ASN_NID).map(
            dataContainerChild -> new AsNumber((Uint32) dataContainerChild.getValue())).orElse(null);
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
