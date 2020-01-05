/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.esi.types;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.extractLD;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.extractRD;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.EsiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.esi.RouterIdGeneratedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.esi.RouterIdGeneratedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.esi.router.id.generated._case.RouterIdGenerated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.esi.router.id.generated._case.RouterIdGeneratedBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class RouterIdParser extends AbstractEsiType {

    @Override
    public ByteBuf serializeBody(final Esi esi, final ByteBuf body) {
        checkArgument(esi instanceof RouterIdGeneratedCase,
            "Unknown esi instance. Passed %s. Needed RouterIdGeneratedCase.", esi);
        final RouterIdGenerated routerID = ((RouterIdGeneratedCase) esi).getRouterIdGenerated();
        ByteBufWriteUtil.writeIpv4Address(routerID.getRouterId(), body);
        ByteBufUtils.writeOrZero(body, routerID.getLocalDiscriminator());
        return body.writeByte(0);
    }

    @Override
    protected EsiType getType() {
        return EsiType.RouterIdGenerated;
    }

    @Override
    public Esi serializeEsi(final ContainerNode esi) {
        return new RouterIdGeneratedCaseBuilder()
                .setRouterIdGenerated(new RouterIdGeneratedBuilder()
                    .setLocalDiscriminator(extractLD(esi))
                    .setRouterId(extractRD(esi))
                    .build())
                .build();
    }

    @Override
    public Esi parseEsi(final ByteBuf buffer) {
        return new RouterIdGeneratedCaseBuilder()
                .setRouterIdGenerated(new RouterIdGeneratedBuilder()
                    .setRouterId(Ipv4Util.addressForByteBuf(buffer))
                    .setLocalDiscriminator(ByteBufUtils.readUint32(buffer))
                    .build())
                .build();
    }
}
