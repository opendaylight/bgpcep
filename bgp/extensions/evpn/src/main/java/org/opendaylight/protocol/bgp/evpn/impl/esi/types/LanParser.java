/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.esi.types;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.extractBP;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.extractBrigeMac;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.IetfYangUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.EsiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.esi.LanAutoGeneratedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.esi.LanAutoGeneratedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.esi.lan.auto.generated._case.LanAutoGenerated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.esi.lan.auto.generated._case.LanAutoGeneratedBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class LanParser extends AbstractEsiType {

    @Override
    public ByteBuf serializeBody(final Esi esi, final ByteBuf body) {
        checkArgument(esi instanceof LanAutoGeneratedCase,
            "Unknown esi instance. Passed %s. Needed LanAutoGeneratedCase.", esi);
        final LanAutoGenerated lan = ((LanAutoGeneratedCase) esi).getLanAutoGenerated();
        body.writeBytes(IetfYangUtil.INSTANCE.bytesFor(lan.getRootBridgeMacAddress()));
        ByteBufWriteUtil.writeUnsignedShort(lan.getRootBridgePriority(), body);
        return body.writeZero(ZERO_BYTE);
    }

    @Override
    protected EsiType getType() {
        return EsiType.LanAutoGenerated;
    }

    @Override
    public Esi serializeEsi(final ContainerNode esi) {
        return new LanAutoGeneratedCaseBuilder()
                .setLanAutoGenerated(new LanAutoGeneratedBuilder()
                    .setRootBridgeMacAddress(extractBrigeMac(esi))
                    .setRootBridgePriority(extractBP(esi))
                    .build())
                .build();
    }

    @Override
    public Esi parseEsi(final ByteBuf buffer) {
        return new LanAutoGeneratedCaseBuilder()
                .setLanAutoGenerated(new LanAutoGeneratedBuilder()
                    .setRootBridgeMacAddress(IetfYangUtil.INSTANCE.macAddressFor(
                        ByteArray.readBytes(buffer, MAC_ADDRESS_LENGTH)))
                    .setRootBridgePriority(buffer.readUnsignedShort())
                    .build())
                .build();
    }
}
