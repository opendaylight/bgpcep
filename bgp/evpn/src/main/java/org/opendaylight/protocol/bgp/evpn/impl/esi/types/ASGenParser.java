/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.esi.types;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedInt;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.EsiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.AsGeneratedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.AsGeneratedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.as.generated._case.AsGenerated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.as.generated._case.AsGeneratedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.Evpn;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

final class ASGenParser extends AbstractEsiType {
    public static final NodeIdentifier AS_GEN_CASE_NID = new NodeIdentifier(AsGeneratedCase.QNAME);
    static final NodeIdentifier AS_NID = NodeIdentifier.create(QName.create(Evpn.QNAME, "as").intern());
    static final NodeIdentifier AS_GEN_NID = new NodeIdentifier(AsGenerated.QNAME);

    @Override
    public void serializeEsi(final Esi esi, final ByteBuf buffer) {
        Preconditions.checkArgument(esi instanceof AsGeneratedCase, "Unknown esi instance. Passed %s. Needed AsGeneratedCase.", esi.getClass());
        final ByteBuf body = Unpooled.buffer(ESI_TYPE_LENGTH);
        final AsGenerated asGen = ((AsGeneratedCase) esi).getAsGenerated();
        body.writeByte(EsiType.AsGenerated.getIntValue());
        writeUnsignedInt(asGen.getAs().getValue(), body);
        writeUnsignedInt(asGen.getLocalDiscriminator(), body);
        body.writeZero(ZERO_BYTE);
        buffer.writeBytes(body);
    }

    @Override
    public Esi serializeEsi(final ContainerNode esi) {
        final AsGeneratedBuilder builder = new AsGeneratedBuilder();
        builder.setLocalDiscriminator(extractLD(esi));
        builder.setAs(extractAS(esi));
        return new AsGeneratedCaseBuilder().setAsGenerated(builder.build()).build();
    }

    private AsNumber extractAS(final ContainerNode asGen) {
        if (asGen.getChild(AS_NID).isPresent()) {
            return new AsNumber((Long) asGen.getChild(AS_NID).get().getValue());
        }
        return null;
    }

    @Override
    public Esi parseEsi(final ByteBuf buffer) {
        final AsGenerated asGen = new AsGeneratedBuilder().setAs(new AsNumber(buffer.readUnsignedInt()))
            .setLocalDiscriminator(buffer.readUnsignedInt()).build();
        return new AsGeneratedCaseBuilder().setAsGenerated(asGen).build();
    }
}
