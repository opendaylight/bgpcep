/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.lsp.setup.type01;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.path.setup.type.tlv.PathSetupTypeBuilder;

public class PcepTlvParserTest {

    private static final byte[] pstTlvBytes = { 0x0, 0x1b, 0x0, 0x4, 0x0, 0x0, 0x0, 0x1 };

    @Test
    public void testPathSetupTypeTlvParser() throws PCEPDeserializerException {
        final PathSetupTypeTlvParser parser = new PathSetupTypeTlvParser();
        final PathSetupType pstTlv = new PathSetupTypeBuilder().setPst(true).build();

        assertEquals(pstTlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(pstTlvBytes, 4))));
        assertArrayEquals(pstTlvBytes, parser.serializeTlv(pstTlv));
    }

}
