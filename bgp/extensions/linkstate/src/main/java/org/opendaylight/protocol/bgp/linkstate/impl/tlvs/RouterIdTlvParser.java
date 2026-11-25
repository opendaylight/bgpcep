/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import io.netty.buffer.ByteBuf;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.linkstate.spi.LinkstateTlvParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.OspfInterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.isis.lan.identifier.IsIsRouterIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.IsisNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.IsisNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.IsisPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.IsisPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.OspfNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.OspfNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.OspfPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.OspfPseudonodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.isis.node._case.IsisNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.ospf.node._case.OspfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public final class RouterIdTlvParser implements LinkstateTlvParser<CRouterIdentifier>,
        LinkstateTlvParser.LinkstateTlvSerializer<CRouterIdentifier> {
    private static final int OSPF_PSEUDONODE_ROUTER_ID_LENGTH = 8;
    private static final int OSPF_ROUTER_ID_LENGTH = 4;
    private static final int ISO_SYSTEM_ID_LENGTH = 6;
    private static final int PSN_LENGTH = 1;
    private static final int IGP_ROUTER_ID = 515;

    @Override
    public void serializeTlvBody(final CRouterIdentifier tlv, final ByteBuf body) {
        switch (tlv) {
            case IsisNodeCase isis -> body.writeBytes(isis.getIsisNode().getIsoSystemId().getValue());
            case IsisPseudonodeCase isis -> {
                final var pseudonode = isis.getIsisPseudonode();
                body.writeBytes(pseudonode.getIsIsRouterIdentifier().getIsoSystemId().getValue());
                ByteBufUtils.writeOrZero(body, pseudonode.getPsn());
            }
            case OspfNodeCase ospf -> ByteBufUtils.writeOrZero(body, ospf.getOspfNode().getOspfRouterId());
            case OspfPseudonodeCase ospf -> {
                final var pseudonode = ospf.getOspfPseudonode();
                ByteBufUtils.writeOrZero(body, pseudonode.getOspfRouterId());
                ByteBufUtils.write(body, pseudonode.getLanInterface().getValue());
            }
            case null, default -> {
                // no-op
            }
        }
    }

    @Override
    public CRouterIdentifier parseTlvBody(final ByteBuf value) {
        return switch (value.readableBytes()) {
            case ISO_SYSTEM_ID_LENGTH -> newIsisNodeCase(readSystemId(value));
            case ISO_SYSTEM_ID_LENGTH + PSN_LENGTH -> {
                final var systemId = readSystemId(value);
                final var psn = ByteBufUtils.readUint8(value);
                yield psn.equals(Uint8.ZERO) ? newIsisNodeCase(systemId)
                    : new IsisPseudonodeCaseBuilder()
                        .setIsisPseudonode(new IsisPseudonodeBuilder()
                            .setIsIsRouterIdentifier(new IsIsRouterIdentifierBuilder().setIsoSystemId(systemId).build())
                            .setPsn(psn)
                            .build())
                        .build();
            }
            case OSPF_ROUTER_ID_LENGTH -> new OspfNodeCaseBuilder()
                .setOspfNode(new OspfNodeBuilder().setOspfRouterId(ByteBufUtils.readUint32(value)).build())
                .build();
            case OSPF_PSEUDONODE_ROUTER_ID_LENGTH -> new OspfPseudonodeCaseBuilder()
                .setOspfPseudonode(new OspfPseudonodeBuilder()
                    .setOspfRouterId(ByteBufUtils.readUint32(value))
                    .setLanInterface(new OspfInterfaceIdentifier(ByteBufUtils.readUint32(value)))
                    .build())
                .build();
            default -> null;
        };
    }

    @NonNullByDefault
    private static IsisNodeCase newIsisNodeCase(final IsoSystemIdentifier systemId) {
        return new IsisNodeCaseBuilder().setIsisNode(new IsisNodeBuilder().setIsoSystemId(systemId).build()).build();
    }

    @NonNullByDefault
    private static IsoSystemIdentifier readSystemId(final ByteBuf value) {
        return new IsoSystemIdentifier(ByteArray.readBytes(value, ISO_SYSTEM_ID_LENGTH));
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
