package org.opendaylight.protocol.bgp.flowspec;

import static org.junit.Assert.*;

import io.netty.buffer.Unpooled;
import org.junit.Test;

public class AbstractFlowspecNlriParserTest {

    @Test
    public void readNlriLength() throws Exception {
        assertEquals(0, AbstractFlowspecNlriParser.readNlriLength(Unpooled.wrappedBuffer(new byte[] { 0x00, 0x0 })));
        assertEquals(1, AbstractFlowspecNlriParser.readNlriLength(Unpooled.wrappedBuffer(new byte[] { 0x01, 0x1 })));
        assertEquals(240, AbstractFlowspecNlriParser.readNlriLength(Unpooled.wrappedBuffer(new byte[] { (byte) 0xf0, (byte)0xf0 })));
        assertEquals(241, AbstractFlowspecNlriParser.readNlriLength(Unpooled.wrappedBuffer(new byte[] { (byte) 0xf0, (byte)0xf1 })));
        assertEquals(4095, AbstractFlowspecNlriParser.readNlriLength(Unpooled.wrappedBuffer(new byte[] { (byte) 0xff, (byte)0xff })));
    }

}
