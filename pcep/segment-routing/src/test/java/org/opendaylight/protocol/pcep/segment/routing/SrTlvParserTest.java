/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.junit.Test;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.sr.pce.capability.tlv.SrPceCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.sr.pce.capability.tlv.SrPceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.setup.type.tlv.PathSetupTypeBuilder;

public class SrTlvParserTest {

    private static final byte[] SPC_TLV_BYTES = { 0x0, 0x1a, 0x0, 0x4, 0x0, 0x0, 0x0, 0x1 };

    private static final byte[] SR_TE_PST_BYTES = { 0x0, 0x1C, 0x0, 0x4, 0x0, 0x0, 0x0, 0x1 };

    @Test
    public void testSrPceCapabilityParser() throws PCEPDeserializerException {
        final SrPceCapabilityTlvParser parser = new SrPceCapabilityTlvParser();
        final SrPceCapability spcTlv = new SrPceCapabilityBuilder().setMsd((short) 1).build();
        assertEquals(spcTlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(SPC_TLV_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(spcTlv, buff);
        assertArrayEquals(SPC_TLV_BYTES, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testPathSetupTypeTlvParser() throws PCEPDeserializerException {
        final SrPathSetupTypeTlvParser parser = new SrPathSetupTypeTlvParser();
        final PathSetupType pstTlv = new PathSetupTypeBuilder().setPst((short) 1).build();
        assertEquals(pstTlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(SR_TE_PST_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(pstTlv, buff);
        assertArrayEquals(SR_TE_PST_BYTES, ByteArray.getAllBytes(buff));
    }

}
