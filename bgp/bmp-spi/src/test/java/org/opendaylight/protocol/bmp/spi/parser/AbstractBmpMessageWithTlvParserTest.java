package org.opendaylight.protocol.bmp.spi.parser;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.opendaylight.protocol.bmp.spi.registry.SimpleBmpTlvRegistryTest.bmpTlvParser;
import static org.opendaylight.protocol.bmp.spi.registry.SimpleBmpTlvRegistryTest.bmpTlvSerializer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.bmp.spi.registry.BmpTlvRegistry;
import org.opendaylight.protocol.bmp.spi.registry.SimpleBmpTlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.description.tlv.DescriptionTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.description.tlv.DescriptionTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.initiation.TlvsBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

public class AbstractBmpMessageWithTlvParserTest {

    private SimpleBmpTlvRegistry registry = new SimpleBmpTlvRegistry();
    private SimpleHandler handler = new SimpleHandler(registry);
    private final byte[] DATA = { 0, 1, 0, 4, 't', 'e', 's', 't' };

    @Test
    public void testParseTlvs() throws BmpDeserializationException {
        final byte[] data = { };
        TlvsBuilder builder = new TlvsBuilder();
        handler.parseTlvs(builder, Unpooled.wrappedBuffer(data));
        assertNull(builder.getDescriptionTlv());

        registry.registerBmpTlvParser(1, bmpTlvParser);
        handler.parseTlvs(builder, Unpooled.wrappedBuffer(DATA));
        assertNotNull(builder.getDescriptionTlv());
    }

    @Test
    public void testSerializeTlv() {
        final ByteBuf output = Unpooled.buffer();
        final ByteBuf expected = Unpooled.buffer();
        DescriptionTlvBuilder builder = new DescriptionTlvBuilder().setDescription("test");
        handler.serializeTlv(builder.build(), output);
        assertArrayEquals(ByteArray.getAllBytes(expected), ByteArray.getAllBytes(output));

        registry.registerBmpTlvSerializer(DescriptionTlv.class, bmpTlvSerializer);
        handler.serializeTlv(builder.build(), output);
        assertArrayEquals(DATA, ByteArray.getAllBytes(output));
    }

    @Test(expected=BmpDeserializationException.class)
    public void testParseCorruptedTlv() throws BmpDeserializationException {
        final byte[] badData = { 0, 1, 0, 10, 't', 'e', 's', 't' };
        registry.registerBmpTlvParser(1, bmpTlvParser);
        handler.parseTlvs(new TlvsBuilder(), Unpooled.wrappedBuffer(badData));
    }

    private static final class SimpleHandler extends AbstractBmpMessageWithTlvParser<TlvsBuilder> {

        public SimpleHandler(BmpTlvRegistry tlvRegistry) {
            super(tlvRegistry);
        }

        @Override
        public void serializeMessageBody(Notification message, ByteBuf buffer) {

        }

        @Override
        public Notification parseMessageBody(ByteBuf bytes)
                throws BmpDeserializationException {
            return null;
        }

        @Override
        public int getBmpMessageType() {
            return 0;
        }

        @Override
        protected void addTlv(TlvsBuilder builder, Tlv tlv) {
            if(tlv != null && tlv instanceof DescriptionTlv) {
                builder.setDescriptionTlv((DescriptionTlv)tlv);
            }
        }
    }
}
