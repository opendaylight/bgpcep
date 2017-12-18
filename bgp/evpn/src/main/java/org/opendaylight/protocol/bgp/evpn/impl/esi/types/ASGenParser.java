/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.esi.types;

import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.extractAS;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.extractLD;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.EsiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.AsGeneratedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.AsGeneratedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.as.generated._case.AsGenerated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.as.generated._case.AsGeneratedBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class ASGenParser extends AbstractEsiType {

    @Override
    public void serializeBody(final Esi esi, final ByteBuf body) {
        Preconditions.checkArgument(esi instanceof AsGeneratedCase,
                "Unknown esi instance. Passed %s. Needed AsGeneratedCase.", esi.getClass());
        final AsGenerated asGen = ((AsGeneratedCase) esi).getAsGenerated();
        writeUnsignedInt(asGen.getAs().getValue(), body);
        writeUnsignedInt(asGen.getLocalDiscriminator(), body);
        body.writeZero(ZERO_BYTE);
    }

    @Override
    protected EsiType getType() {
        return EsiType.AsGenerated;
    }

    @Override
    public Esi serializeEsi(final ContainerNode esi) {
        final AsGeneratedBuilder builder = new AsGeneratedBuilder();
        builder.setLocalDiscriminator(extractLD(esi));
        builder.setAs(extractAS(esi));
        return new AsGeneratedCaseBuilder().setAsGenerated(builder.build()).build();
    }

    @Override
    public Esi parseEsi(final ByteBuf buffer) {
        final AsGenerated asGen = new AsGeneratedBuilder().setAs(new AsNumber(buffer.readUnsignedInt()))
            .setLocalDiscriminator(buffer.readUnsignedInt()).build();
        return new AsGeneratedCaseBuilder().setAsGenerated(asGen).build();
    }
}
