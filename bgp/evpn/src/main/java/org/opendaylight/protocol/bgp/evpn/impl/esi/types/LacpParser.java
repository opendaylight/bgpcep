/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.esi.types;

import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.extractLacpMac;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.EsiModelUtil.extractPK;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.IetfYangUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.EsiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.LacpAutoGeneratedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.LacpAutoGeneratedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.lacp.auto.generated._case.LacpAutoGenerated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.esi.lacp.auto.generated._case.LacpAutoGeneratedBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class LacpParser extends AbstractEsiType {

    @Override
    public void serializeBody(final Esi esi, final ByteBuf body) {
        Preconditions.checkArgument(esi instanceof LacpAutoGeneratedCase,
                "Unknown esi instance. Passed %s. Needed LacpAutoGeneratedCase.", esi.getClass());
        final LacpAutoGenerated lacp = ((LacpAutoGeneratedCase) esi).getLacpAutoGenerated();
        body.writeBytes(IetfYangUtil.INSTANCE.bytesFor(lacp.getCeLacpMacAddress()));
        ByteBufWriteUtil.writeUnsignedShort(lacp.getCeLacpPortKey(), body);
        body.writeZero(ZERO_BYTE);
    }

    @Override
    protected EsiType getType() {
        return EsiType.LacpAutoGenerated;
    }

    @Override
    public Esi serializeEsi(final ContainerNode esi) {
        final LacpAutoGeneratedBuilder builder = new LacpAutoGeneratedBuilder();
        builder.setCeLacpMacAddress(extractLacpMac(esi));
        builder.setCeLacpPortKey(extractPK(esi));
        return new LacpAutoGeneratedCaseBuilder().setLacpAutoGenerated(builder.build()).build();

    }

    @Override
    public Esi parseEsi(final ByteBuf buffer) {
        final LacpAutoGenerated t1 = new LacpAutoGeneratedBuilder()
            .setCeLacpMacAddress(IetfYangUtil.INSTANCE.macAddressFor(ByteArray.readBytes(buffer, MAC_ADDRESS_LENGTH)))
            .setCeLacpPortKey(buffer.readUnsignedShort())
            .build();
        return new LacpAutoGeneratedCaseBuilder().setLacpAutoGenerated(t1).build();
    }
}
