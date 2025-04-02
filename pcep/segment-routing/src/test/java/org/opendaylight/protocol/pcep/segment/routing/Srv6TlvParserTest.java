/*
 * Copyright (c) 2025 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.segment.routing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.srv6.pce.capability.tlv.Srv6PceCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.srv6.pce.capability.tlv.Srv6PceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.srv6.pce.capability.tlv.srv6.pce.capability.MsdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250328.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250328.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yangtools.yang.common.Uint8;

public class Srv6TlvParserTest {

    private static final byte[] SRV6_PC_TLV_BYTES = { 0x0, 0x1b, 0x0, 0x6, 0x0, 0x0, 0x0, 0x2, 0x1, 0x0a, 0x00, 0x00 };

    private static final byte[] SRV6_TE_PST_BYTES = { 0x0, 0x1c, 0x0, 0x4, 0x0, 0x0, 0x0, 0x3 };

    @Test
    public void testSrv6PceCapabilityParser() throws PCEPDeserializerException {
        final Srv6PceCapabilityTlvParser parser = new Srv6PceCapabilityTlvParser();
        final Srv6PceCapability spcTlv = new Srv6PceCapabilityBuilder().setNFlag(Boolean.TRUE)
            .setMsds(List.of(new MsdsBuilder().setMsdType(Uint8.ONE).setMsdValue(Uint8.TEN).build())).build();
        assertEquals(spcTlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(SRV6_PC_TLV_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(spcTlv, buff);
        assertArrayEquals(SRV6_PC_TLV_BYTES, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testPathSetupTypeTlvParser() throws PCEPDeserializerException {
        final Srv6PathSetupTypeTlvParser parser = new Srv6PathSetupTypeTlvParser();
        final PathSetupType pstTlv = new PathSetupTypeBuilder().setPst(Uint8.valueOf(3)).build();
        assertEquals(pstTlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(SRV6_TE_PST_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(pstTlv, buff);
        assertArrayEquals(SRV6_TE_PST_BYTES, ByteArray.getAllBytes(buff));
    }

}
