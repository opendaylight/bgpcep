/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.esi.types;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.extractSystmeMac;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.extractUint24LD;

import io.netty.buffer.ByteBuf;
import org.opendaylight.mdsal.uint24.netty.Uint24ByteBufUtils;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.IetfYangUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.EsiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.esi.MacAutoGeneratedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.esi.MacAutoGeneratedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.esi.mac.auto.generated._case.MacAutoGenerated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev180329.esi.esi.mac.auto.generated._case.MacAutoGeneratedBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class MacParser extends AbstractEsiType {

    @Override
    public ByteBuf serializeBody(final Esi esi, final ByteBuf body) {
        checkArgument(esi instanceof MacAutoGeneratedCase,
            "Unknown esi instance. Passed %s. Needed MacAutoGeneratedCase.", esi.getClass());
        final MacAutoGenerated macAuto = ((MacAutoGeneratedCase) esi).getMacAutoGenerated();
        body.writeBytes(IetfYangUtil.INSTANCE.macAddressBytes(macAuto.getSystemMacAddress()));
        Uint24ByteBufUtils.writeUint24(body, macAuto.getLocalDiscriminator());
        return body;
    }

    @Override
    protected EsiType getType() {
        return EsiType.MacAutoGenerated;
    }

    @Override
    public Esi serializeEsi(final ContainerNode esi) {
        return new MacAutoGeneratedCaseBuilder()
                .setMacAutoGenerated(new MacAutoGeneratedBuilder()
                    .setSystemMacAddress(extractSystmeMac(esi))
                    .setLocalDiscriminator(extractUint24LD(esi))
                    .build())
                .build();
    }

    @Override
    public Esi parseEsi(final ByteBuf buffer) {
        return new MacAutoGeneratedCaseBuilder()
                .setMacAutoGenerated(new MacAutoGeneratedBuilder()
                    .setSystemMacAddress(IetfYangUtil.INSTANCE.macAddressFor(
                        ByteArray.readBytes(buffer, MAC_ADDRESS_LENGTH)))
                    .setLocalDiscriminator(Uint24ByteBufUtils.readUint24(buffer))
                    .build())
                .build();
    }
}
