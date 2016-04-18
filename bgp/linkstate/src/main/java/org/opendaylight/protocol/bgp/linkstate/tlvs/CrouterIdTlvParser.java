/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.SimpleNlriTypeRegistry;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.OspfInterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.isis.lan.identifier.IsIsRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.isis.lan.identifier.IsIsRouterIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.isis.node._case.IsisNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.ospf.node._case.OspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CrouterIdTlvParser implements NlriSubTlvObjectParser, NlriSubTlvObjectSerializer, SubTlvNodeBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(CrouterIdTlvParser.class);

    private static final int OSPF_PSEUDONODE_ROUTER_ID_LENGTH = 8;
    private static final int OSPF_ROUTER_ID_LENGTH = 4;
    private static final int ISO_SYSTEM_ID_LENGTH = 6;
    private static final int PSN_LENGTH = 1;

    public static final int IGP_ROUTER_ID = 515;

    @Override
    public Object parseNlriSubTlvObject(final ByteBuf value, final NlriType nlriType) throws BGPParsingException {

        if (value.readableBytes() == ISO_SYSTEM_ID_LENGTH || (value.readableBytes() == ISO_SYSTEM_ID_LENGTH + PSN_LENGTH && value.getByte(ISO_SYSTEM_ID_LENGTH) == 0)) {
            final IsisNodeCase IsisNCaseId = new IsisNodeCaseBuilder().setIsisNode(
                    new IsisNodeBuilder().setIsoSystemId(new IsoSystemIdentifier(ByteArray.readBytes(value, ISO_SYSTEM_ID_LENGTH))).build()).build();
            LOG.debug("Parsed IsisNodeCase Router Identifier {}", IsisNCaseId);
            return IsisNCaseId;
        }
        if (value.readableBytes() == ISO_SYSTEM_ID_LENGTH + PSN_LENGTH) {
            final IsIsRouterIdentifier iri = new IsIsRouterIdentifierBuilder().setIsoSystemId(
                    new IsoSystemIdentifier(ByteArray.readBytes(value, ISO_SYSTEM_ID_LENGTH))).build();
            final IsisPseudonodeCase IsisPsNCaseId = new IsisPseudonodeCaseBuilder().setIsisPseudonode(new IsisPseudonodeBuilder().setIsIsRouterIdentifier(iri).setPsn((short) value.readByte()).build()).build();
            LOG.debug("Parsed IsisPseudoNodeCase Router Identifier {}", IsisPsNCaseId);
            return IsisPsNCaseId;
        }
        if (value.readableBytes() == OSPF_ROUTER_ID_LENGTH) {
            final OspfNodeCase OspfNCaseId = new OspfNodeCaseBuilder().setOspfNode(
                    new OspfNodeBuilder().setOspfRouterId(value.readUnsignedInt()).build()).build();
            LOG.debug("Parsed OspfNodeCase Router Identifier {}", OspfNCaseId);
            return OspfNCaseId;

        }
        if (value.readableBytes() == OSPF_PSEUDONODE_ROUTER_ID_LENGTH) {
            final OspfPseudonodeCase OspfPsNCaseId = new OspfPseudonodeCaseBuilder().setOspfPseudonode(
                    new OspfPseudonodeBuilder().setOspfRouterId(value.readUnsignedInt()).setLanInterface(new OspfInterfaceIdentifier(value.readUnsignedInt())).build()).build();
            LOG.debug("Parsed OspfPseudoNodeCase Router Identifier {}", OspfPsNCaseId);
            return OspfPsNCaseId;
        }
        throw new BGPParsingException("Router Id of invalid length " + value.readableBytes());
    }

    @Override
    public void serializeNlriSubTlvObject(final ObjectType nlriTypeCase, final NodeIdentifier qNameId, final ByteBuf buffer) {
        if (qNameId.equals(LinkstateNlriParser.NODE_DESCRIPTORS_NID)) {
            final NodeDescriptors nodeDescriptors = ((NodeCase)nlriTypeCase).getNodeDescriptors();
            if (nodeDescriptors.getCRouterIdentifier() != null) {
                final ByteBuf routerIdBuf = Unpooled.buffer();
                SimpleNlriTypeRegistry.getInstance().serializeRouterId(nodeDescriptors.getCRouterIdentifier(), routerIdBuf);
                TlvUtil.writeTLV(IGP_ROUTER_ID, routerIdBuf, buffer);
            }
        } else if (qNameId.equals(LinkstateNlriParser.LOCAL_NODE_DESCRIPTORS_NID)) {
            final LocalNodeDescriptors localNodeDesc = ((LinkCase)nlriTypeCase).getLocalNodeDescriptors();
            if (localNodeDesc.getCRouterIdentifier() != null) {
                final ByteBuf routerIdBuf = Unpooled.buffer();
                SimpleNlriTypeRegistry.getInstance().serializeRouterId(localNodeDesc.getCRouterIdentifier(), routerIdBuf);
                TlvUtil.writeTLV(IGP_ROUTER_ID, routerIdBuf, buffer);
            }
        } else if (qNameId.equals(LinkstateNlriParser.REMOTE_NODE_DESCRIPTORS_NID))  {
            final RemoteNodeDescriptors remNodeDesc = ((LinkCase)nlriTypeCase).getRemoteNodeDescriptors();
            if (remNodeDesc.getCRouterIdentifier() != null) {
                final ByteBuf routerIdBuf = Unpooled.buffer();
                SimpleNlriTypeRegistry.getInstance().serializeRouterId(remNodeDesc.getCRouterIdentifier(), routerIdBuf);
                TlvUtil.writeTLV(IGP_ROUTER_ID, routerIdBuf, buffer);
            }
        } else if (qNameId.equals(LinkstateNlriParser.ADVERTISING_NODE_DESCRIPTORS_NID)) {
            final AdvertisingNodeDescriptors advertNodeDesc = ((PrefixCase)nlriTypeCase).getAdvertisingNodeDescriptors();
            if (advertNodeDesc.getCRouterIdentifier() != null) {
                final ByteBuf routerIdBuf = Unpooled.buffer();
                SimpleNlriTypeRegistry.getInstance().serializeRouterId(advertNodeDesc.getCRouterIdentifier(), routerIdBuf);
                TlvUtil.writeTLV(IGP_ROUTER_ID, routerIdBuf, buffer);
            }
        }
    }

    @Override
    public void buildNodeDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((NodeDescriptorsBuilder) tlvBuilder).setCRouterIdentifier((CRouterIdentifier) subTlvObject);
    }

    @Override
    public void buildLocalNodeDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((LocalNodeDescriptorsBuilder) tlvBuilder).setCRouterIdentifier((CRouterIdentifier) subTlvObject);
    }

    @Override
    public void buildRemoteNodeDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((RemoteNodeDescriptorsBuilder) tlvBuilder).setCRouterIdentifier((CRouterIdentifier) subTlvObject);
    }

    @Override
    public void buildAdvertisingNodeDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((AdvertisingNodeDescriptorsBuilder) tlvBuilder).setCRouterIdentifier((CRouterIdentifier) subTlvObject);
    }
}
