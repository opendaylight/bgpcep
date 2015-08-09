/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.spi.parser;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bmp.spi.registry.SimpleBmpTlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.description.tlv.DescriptionTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.description.tlv.DescriptionTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.initiation.TlvsBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

public class AbstractBmpMessageWithTlvParserTest {

    private final SimpleBmpTlvRegistry registry = new SimpleBmpTlvRegistry();
    private final SimpleHandler parser = new SimpleHandler(this.registry);
    private static final byte[] DATA = { 0, 1, 0, 4, 't', 'e', 's', 't' };
    private static final int TYPE = 1;

    public static final BmpTlvSerializer DESCRIPTION_TLV_SERIALIZER = new BmpTlvSerializer() {
        @Override
        public void serializeTlv(final Tlv tlv, final ByteBuf output) {
            Preconditions.checkArgument(tlv instanceof DescriptionTlv, "DescriptionTlv is mandatory.");
            TlvUtil.formatTlvAscii(TYPE, ((DescriptionTlv) tlv).getDescription(), output);
        }
    };

    public static final BmpTlvParser DESCRIPTION_TLV_PARSER = new BmpTlvParser() {
        @Override
        public Tlv parseTlv(final ByteBuf buffer) throws BmpDeserializationException {
            if (buffer == null) {
                return null;
            }
            return new DescriptionTlvBuilder().setDescription(buffer.toString(Charsets.US_ASCII)).build();
        }
    };

    @Before
    public void setUp() {
        this.registry.registerBmpTlvParser(TYPE, DESCRIPTION_TLV_PARSER);
        this.registry.registerBmpTlvSerializer(DescriptionTlv.class, DESCRIPTION_TLV_SERIALIZER);
    }

    @Test
    public void testParseTlvs() throws BmpDeserializationException {
        final ByteBuf buffer = Unpooled.EMPTY_BUFFER;
        final TlvsBuilder builder = new TlvsBuilder();
        this.parser.parseTlvs(builder, buffer);
        assertNull(builder.getDescriptionTlv());

        this.parser.parseTlvs(builder, Unpooled.wrappedBuffer(DATA));
        assertNotNull(builder.getDescriptionTlv());
        assertEquals("test", builder.getDescriptionTlv().getDescription());
    }

    @Test
    public void testSerializeTlv() {
        final ByteBuf output = Unpooled.buffer();
        final DescriptionTlvBuilder builder = new DescriptionTlvBuilder().setDescription("test");
        this.parser.serializeTlv(builder.build(), output);
        assertArrayEquals(DATA, ByteArray.getAllBytes(output));
    }

    @Test(expected=BmpDeserializationException.class)
    public void testParseCorruptedTlv() throws BmpDeserializationException {
        final byte[] wrongData = { 0, 1, 0, 10, 't', 'e', 's', 't' };
        this.parser.parseTlvs(new TlvsBuilder(), Unpooled.wrappedBuffer(wrongData));
    }

    private static final class SimpleHandler extends AbstractBmpMessageWithTlvParser<TlvsBuilder> {
        public SimpleHandler(final BmpTlvRegistry tlvRegistry) {
            super(tlvRegistry);
        }
        @Override
        public void serializeMessageBody(final Notification message, final ByteBuf buffer) {
        }
        @Override
        public Notification parseMessageBody(final ByteBuf bytes) throws BmpDeserializationException {
            return null;
        }
        @Override
        public int getBmpMessageType() {
            return 0;
        }
        @Override
        protected void addTlv(final TlvsBuilder builder, final Tlv tlv) {
            if(tlv != null && tlv instanceof DescriptionTlv) {
                builder.setDescriptionTlv((DescriptionTlv)tlv);
            }
        }
    }
}
