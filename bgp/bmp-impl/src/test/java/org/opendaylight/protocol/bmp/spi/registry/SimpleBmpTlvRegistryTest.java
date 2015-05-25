package org.opendaylight.protocol.bmp.spi.registry;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.bmp.impl.tlv.DescriptionTlvHandler;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.description.tlv.DescriptionTlvBuilder;

public class SimpleBmpTlvRegistryTest {

    final SimpleBmpTlvRegistry tlvRegistry = new SimpleBmpTlvRegistry();

    @Test
    public void testSerializeTlv() throws BmpDeserializationException {
        final Tlv emptyTlv = tlvRegistry.parseTlv(DescriptionTlvHandler.TYPE, Unpooled.EMPTY_BUFFER);
        assertNull(emptyTlv);
    }

    @Test
    public void testParseTlv() {
        final ByteBuf serializedData = Unpooled.buffer();
        final DescriptionTlvBuilder descTlvBuilder = new DescriptionTlvBuilder();
        descTlvBuilder.setDescription("test");
        tlvRegistry.serializeTlv(descTlvBuilder.build(), serializedData);
        assertArrayEquals(new byte[] { }, ByteArray.getAllBytes(serializedData));
    }
}
