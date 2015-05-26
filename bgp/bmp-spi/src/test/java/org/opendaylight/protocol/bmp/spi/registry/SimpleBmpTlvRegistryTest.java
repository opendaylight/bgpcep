package org.opendaylight.protocol.bmp.spi.registry;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvSerializer;
import org.opendaylight.protocol.bmp.spi.parser.TlvUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.description.tlv.DescriptionTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.description.tlv.DescriptionTlvBuilder;

public class SimpleBmpTlvRegistryTest {

    final SimpleBmpTlvRegistry bmpTlvRegistry = new SimpleBmpTlvRegistry();
    final ByteBuf emptyBuffer = Unpooled.EMPTY_BUFFER;

    public static final BmpTlvSerializer bmpTlvSerializer = new BmpTlvSerializer() {
        @Override
        public void serializeTlv(Tlv tlv, ByteBuf output) {
            if (tlv instanceof DescriptionTlv) {
                DescriptionTlv descTlv = (DescriptionTlv) tlv;
                TlvUtil.formatTlvUtf8(1, descTlv.getDescription(), output);
            }
        }
    };

    public static final BmpTlvParser bmpTlvParser = new BmpTlvParser() {
        @Override
        public Tlv parseTlv(ByteBuf buffer) throws BmpDeserializationException {
            DescriptionTlvBuilder tlvBuilder = new DescriptionTlvBuilder();
            tlvBuilder.setDescription(buffer.toString(Charsets.UTF_8));
            return tlvBuilder.build();
        }
    };

    @Test
    public void testParseTlv() throws BmpDeserializationException {
        final ByteBuf dataToParse = Unpooled.buffer();
        bmpTlvRegistry.parseTlv(1, dataToParse);
        assertArrayEquals(ByteArray.getAllBytes(emptyBuffer), ByteArray.getAllBytes(dataToParse));

        bmpTlvRegistry.registerBmpTlvParser(1, bmpTlvParser);
        Tlv descTlv = bmpTlvRegistry.parseTlv(1, Unpooled.wrappedBuffer(new byte[] { 't', 'e', 's', 't' }));
        assertEquals("test", ((DescriptionTlv)descTlv).getDescription());
    }

    @Test
    public void testSerializeTlv() {
        final ByteBuf serializedData = Unpooled.buffer();
        final DescriptionTlvBuilder descTlvBuilder = new DescriptionTlvBuilder();
        descTlvBuilder.setDescription("test");
        bmpTlvRegistry.serializeTlv(descTlvBuilder.build(), serializedData);
        assertArrayEquals(ByteArray.getAllBytes(emptyBuffer), ByteArray.getAllBytes(serializedData));

        final byte[] expectedData = new byte[] {
            (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x74, (byte) 0x65, (byte) 0x73, (byte) 0x74
        };
        bmpTlvRegistry.registerBmpTlvSerializer(DescriptionTlv.class, bmpTlvSerializer);
        bmpTlvRegistry.serializeTlv(descTlvBuilder.build(), serializedData);
        assertArrayEquals(expectedData, ByteArray.getAllBytes(serializedData));
    }
}
