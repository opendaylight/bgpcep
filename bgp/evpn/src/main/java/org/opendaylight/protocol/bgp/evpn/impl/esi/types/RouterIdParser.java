/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.esi.types;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.EsiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.RouterIdGeneratedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.RouterIdGeneratedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.router.id.generated._case.RouterIdGenerated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.router.id.generated._case.RouterIdGeneratedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.Evpn;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class RouterIdParser extends AbstractEsiType {
    public static final NodeIdentifier ROUTER_ID_CASE_NID = new NodeIdentifier(RouterIdGeneratedCase.QNAME);
    static final NodeIdentifier ROUTER_ID_NID = new NodeIdentifier(RouterIdGenerated.QNAME);
    static final NodeIdentifier RD_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "router-id").intern());

    @Override
    public void serializeEsi(final Esi esi, final ByteBuf buffer) {
        Preconditions.checkArgument(esi instanceof RouterIdGeneratedCase, "Unknown esi instance. Passed %s. Needed RouterIdGeneratedCase.", esi.getClass());
        final ByteBuf body = Unpooled.buffer(ESI_TYPE_LENGTH);
        final RouterIdGenerated routerID = ((RouterIdGeneratedCase) esi).getRouterIdGenerated();
        body.writeByte(EsiType.RouterIdGenerated.getIntValue());
        ByteBufWriteUtil.writeIpv4Address(routerID.getRouterId(), body);
        ByteBufWriteUtil.writeUnsignedInt(routerID.getLocalDiscriminator(), body);
        body.writeZero(ZERO_BYTE);
        buffer.writeBytes(body);
    }

    @Override
    public Esi serializeEsi(final ContainerNode esi) {
        final RouterIdGeneratedBuilder builder = new RouterIdGeneratedBuilder();
        builder.setLocalDiscriminator(extractLD(esi));
        builder.setRouterId(extractRD(esi));
        return new RouterIdGeneratedCaseBuilder().setRouterIdGenerated(builder.build()).build();
    }

    private Ipv4Address extractRD(final ContainerNode t4) {
        if (t4.getChild(RD_NID).isPresent()) {
            return (Ipv4Address) t4.getChild(RD_NID).get().getValue();
        }
        return null;
    }

    @Override
    public Esi parseEsi(final ByteBuf buffer) {
        final RouterIdGenerated routerID = new RouterIdGeneratedBuilder().setRouterId(Ipv4Util.addressForByteBuf(buffer))
            .setLocalDiscriminator(buffer.readUnsignedInt()).build();
        return new RouterIdGeneratedCaseBuilder().setRouterIdGenerated(routerID).build();
    }
}
