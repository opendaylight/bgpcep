/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;

import com.google.common.primitives.UnsignedInteger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.AreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.OspfInterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.isis.lan.identifier.IsIsRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.isis.lan.identifier.IsIsRouterIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.c.linkstate.destination.LocalNodeDescriptorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.c.linkstate.destination.RemoteNodeDescriptorsBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.ospf.node._case.OspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
final class NodeNlriParser {

    private static final Logger LOG = LoggerFactory.getLogger(NodeNlriParser.class);

    private static final int OSPF_PSEUDONODE_ROUTER_ID_LENGTH = 8;
    private static final int OSPF_ROUTER_ID_LENGTH = 4;
    private static final int ISO_SYSTEM_ID_LENGTH = 6;
    private static final int PSN_LENGTH = 1;

    /* Node Descriptor TLVs */
    private static final int AS_NUMBER = 512;
    private static final int BGP_LS_ID = 513;
    private static final int AREA_ID = 514;
    private static final int IGP_ROUTER_ID = 515;

    static NodeIdentifier parseNodeDescriptors(final ByteBuf buffer, final boolean local) throws BGPParsingException {
        AsNumber asnumber = null;
        DomainIdentifier bgpId = null;
        AreaIdentifier ai = null;
        CRouterIdentifier routerId = null;
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            final ByteBuf value = buffer.slice(buffer.readerIndex(), length);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Parsing Node Descriptor: {}", ByteBufUtil.hexDump(value));
            }
            switch (type) {
            case AS_NUMBER:
                asnumber = new AsNumber(value.readUnsignedInt());
                LOG.debug("Parsed {}", asnumber);
                break;
            case BGP_LS_ID:
                bgpId = new DomainIdentifier(value.readUnsignedInt());
                LOG.debug("Parsed {}", bgpId);
                break;
            case AREA_ID:
                ai = new AreaIdentifier(value.readUnsignedInt());
                LOG.debug("Parsed area identifier {}", ai);
                break;
            case IGP_ROUTER_ID:
                routerId = parseRouterId(value);
                LOG.debug("Parsed Router Identifier {}", routerId);
                break;
            default:
                throw new BGPParsingException("Node Descriptor not recognized, type: " + type);
            }
            buffer.skipBytes(length);
        }
        LOG.trace("Finished parsing Node descriptors.");
        return (local) ? new LocalNodeDescriptorsBuilder().setAsNumber(asnumber).setDomainId(bgpId).setAreaId(ai).setCRouterIdentifier(
            routerId).build()
            : new RemoteNodeDescriptorsBuilder().setAsNumber(asnumber).setDomainId(bgpId).setAreaId(ai).setCRouterIdentifier(routerId).build();
    }

    private static CRouterIdentifier parseRouterId(final ByteBuf value) throws BGPParsingException {
        if (value.readableBytes() == ISO_SYSTEM_ID_LENGTH || (value.readableBytes() == ISO_SYSTEM_ID_LENGTH + PSN_LENGTH && value.getByte(ISO_SYSTEM_ID_LENGTH) == 0)) {
            return new IsisNodeCaseBuilder().setIsisNode(
                new IsisNodeBuilder().setIsoSystemId(new IsoSystemIdentifier(ByteArray.readBytes(value, ISO_SYSTEM_ID_LENGTH))).build()).build();
        }
        if (value.readableBytes() == ISO_SYSTEM_ID_LENGTH + PSN_LENGTH) {
            final IsIsRouterIdentifier iri = new IsIsRouterIdentifierBuilder().setIsoSystemId(
                new IsoSystemIdentifier(ByteArray.readBytes(value, ISO_SYSTEM_ID_LENGTH))).build();
            return new IsisPseudonodeCaseBuilder().setIsisPseudonode(new IsisPseudonodeBuilder().setIsIsRouterIdentifier(iri).setPsn((short) value.readByte()).build()).build();
        }
        if (value.readableBytes() == OSPF_ROUTER_ID_LENGTH) {
            return new OspfNodeCaseBuilder().setOspfNode(
                new OspfNodeBuilder().setOspfRouterId(value.readUnsignedInt()).build()).build();
        }
        if (value.readableBytes() == OSPF_PSEUDONODE_ROUTER_ID_LENGTH) {
            return new OspfPseudonodeCaseBuilder().setOspfPseudonode(
                new OspfPseudonodeBuilder().setOspfRouterId(value.readUnsignedInt()).setLanInterface(new OspfInterfaceIdentifier(value.readUnsignedInt())).build()).build();
        }
        throw new BGPParsingException("Router Id of invalid length " + value.readableBytes());
    }

    static void serializeNodeDescriptors(final NodeIdentifier descriptors, final ByteBuf buffer) {
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
}
