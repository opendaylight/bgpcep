/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing02;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.sr.pce.capability.tlv.SrPceCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.sr.pce.capability.tlv.SrPceCapabilityBuilder;

public class PcepTlvParserTest {

    private static final byte[] spcTlvBytes = { 0x0, 0x1a, 0x0, 0x4, 0x0, 0x0, 0x0, 0x1 };

    @Test
    public void testSrPceCapabilityParser() throws PCEPDeserializerException {
        final SrPceCapabilityTlvParser parser = new SrPceCapabilityTlvParser();
        final SrPceCapability spcTlv = new SrPceCapabilityBuilder().setMsd((short) 1).build();
        assertEquals(spcTlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(spcTlvBytes, 4))));
        assertArrayEquals(spcTlvBytes, parser.serializeTlv(spcTlv));
    }

}
