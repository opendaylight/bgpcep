/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.esi.types;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.extractAS;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.extractLD;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.EsiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.esi.AsGeneratedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.esi.AsGeneratedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.esi.as.generated._case.AsGenerated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.esi.as.generated._case.AsGeneratedBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class ASGenParser extends AbstractEsiType {

    @Override
    public ByteBuf serializeBody(final Esi esi, final ByteBuf body) {
        checkArgument(esi instanceof AsGeneratedCase, "Unknown esi instance. Passed %s. Needed AsGeneratedCase.", esi);
        final AsGenerated asGen = ((AsGeneratedCase) esi).getAsGenerated();
        writeUnsignedInt(asGen.getAs().getValue(), body);
        writeUnsignedInt(asGen.getLocalDiscriminator(), body);
        return body.writeByte(0);
    }

    @Override
    protected EsiType getType() {
        return EsiType.AsGenerated;
    }

    @Override
    public Esi serializeEsi(final ContainerNode esi) {
        return new AsGeneratedCaseBuilder()
                .setAsGenerated(new AsGeneratedBuilder()
                    .setAs(extractAS(esi))
                    .setLocalDiscriminator(extractLD(esi))
                    .build())
                .build();
    }

    @Override
    public Esi parseEsi(final ByteBuf buffer) {
        return new AsGeneratedCaseBuilder()
                .setAsGenerated(new AsGeneratedBuilder()
                    .setAs(new AsNumber(ByteBufUtils.readUint32(buffer)))
                    .setLocalDiscriminator(ByteBufUtils.readUint32(buffer))
                    .build())
                .build();
    }
}
