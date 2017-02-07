/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.esi.types;

import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.extractLD;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.extractRD;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.EsiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.RouterIdGeneratedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.RouterIdGeneratedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.router.id.generated._case.RouterIdGenerated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.router.id.generated._case.RouterIdGeneratedBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class RouterIdParser extends AbstractEsiType {

    @Override
    public void serializeBody(final Esi esi, final ByteBuf body) {
        Preconditions.checkArgument(esi instanceof RouterIdGeneratedCase, "Unknown esi instance. Passed %s. Needed RouterIdGeneratedCase.", esi.getClass());
        final RouterIdGenerated routerID = ((RouterIdGeneratedCase) esi).getRouterIdGenerated();
        ByteBufWriteUtil.writeIpv4Address(routerID.getRouterId(), body);
        ByteBufWriteUtil.writeUnsignedInt(routerID.getLocalDiscriminator(), body);
        body.writeZero(ZERO_BYTE);
    }

    @Override
    protected EsiType getType() {
        return EsiType.RouterIdGenerated;
    }

    @Override
    public Esi serializeEsi(final ContainerNode esi) {
        final RouterIdGeneratedBuilder builder = new RouterIdGeneratedBuilder();
        builder.setLocalDiscriminator(extractLD(esi));
        builder.setRouterId(extractRD(esi));
        return new RouterIdGeneratedCaseBuilder().setRouterIdGenerated(builder.build()).build();
    }

    @Override
    public Esi parseEsi(final ByteBuf buffer) {
        final RouterIdGenerated routerID = new RouterIdGeneratedBuilder().setRouterId(Ipv4Util.addressForByteBuf(buffer))
            .setLocalDiscriminator(buffer.readUnsignedInt()).build();
        return new RouterIdGeneratedCaseBuilder().setRouterIdGenerated(routerID).build();
    }
}
