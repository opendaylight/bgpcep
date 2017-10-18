/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.LinkstateTlvParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.OspfInterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.isis.lan.identifier.IsIsRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.isis.lan.identifier.IsIsRouterIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.IsisNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.IsisNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.IsisPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.IsisPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.OspfNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.OspfNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.OspfPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.OspfPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.isis.node._case.IsisNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.isis.node._case.IsisNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.ospf.node._case.OspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yangtools.yang.common.QName;

public final class RouterIdTlvParser implements LinkstateTlvParser<CRouterIdentifier>, LinkstateTlvParser.LinkstateTlvSerializer<CRouterIdentifier> {

    private static final int OSPF_PSEUDONODE_ROUTER_ID_LENGTH = 8;
    private static final int OSPF_ROUTER_ID_LENGTH = 4;
    private static final int ISO_SYSTEM_ID_LENGTH = 6;
    private static final int PSN_LENGTH = 1;

    private static final int IGP_ROUTER_ID = 515;

    @Override
    public void serializeTlvBody(final CRouterIdentifier tlv, final ByteBuf body) {
        if (tlv instanceof IsisNodeCase) {
            final IsisNode isis = ((IsisNodeCase) tlv).getIsisNode();
            body.writeBytes(isis.getIsoSystemId().getValue());
        } else if (tlv instanceof IsisPseudonodeCase) {
            final IsisPseudonode isis = ((IsisPseudonodeCase) tlv).getIsisPseudonode();
            body.writeBytes(isis.getIsIsRouterIdentifier().getIsoSystemId().getValue());
            ByteBufWriteUtil.writeUnsignedByte(isis.getPsn(), body);
        } else if (tlv instanceof OspfNodeCase) {
            ByteBufWriteUtil.writeUnsignedInt(((OspfNodeCase) tlv).getOspfNode().getOspfRouterId(), body);
        } else if (tlv instanceof OspfPseudonodeCase) {
            final OspfPseudonode node = ((OspfPseudonodeCase) tlv).getOspfPseudonode();
            ByteBufWriteUtil.writeUnsignedInt(node.getOspfRouterId(), body);
            ByteBufWriteUtil.writeUnsignedInt(node.getLanInterface().getValue(), body);
        }
    }

    @Override
    public CRouterIdentifier parseTlvBody(final ByteBuf value) {
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
        return null;
    }

    @Override
    public int getType() {
        return IGP_ROUTER_ID;
    }

    @Override
    public QName getTlvQName() {
        return CRouterIdentifier.QNAME;
    }

}

