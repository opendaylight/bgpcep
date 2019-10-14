/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

public class ByteBufUtilsTest {

    @Test
    public void testByte() {
        testWrite(Byte.valueOf((byte) 1), 1, ByteBufUtils::writeOptional, ByteBufUtils::writeOrZero,
            ByteBufUtils::writeMandatory);
    }

    @Test
    public void testShort() {
        testWrite(Short.valueOf((short) 1), 2, ByteBufUtils::writeOptional, ByteBufUtils::writeOrZero,
            ByteBufUtils::writeMandatory);
    }

    @Test
    public void testInteger() {
        testWrite(Integer.valueOf(1), 4, ByteBufUtils::writeOptional, ByteBufUtils::writeOrZero,
            ByteBufUtils::writeMandatory);
    }

    @Test
    public void testLong() {
        testWrite(Long.valueOf(1L), 8, ByteBufUtils::writeOptional, ByteBufUtils::writeOrZero,
            ByteBufUtils::writeMandatory);
    }

    @Test
    public void testUint8() {
        testRead(Uint8.ONE, ByteBufUtils::readUint8, (byte) 1);
        testWrite(Uint8.ONE, 1, ByteBufUtils::writeOptional, ByteBufUtils::writeOrZero, ByteBufUtils::writeMandatory);
    }

    @Test
    public void testUint16() {
        testRead(Uint16.ONE, ByteBufUtils::readUint16, (byte) 0, (byte) 1);
        testWrite(Uint16.ONE, 2, ByteBufUtils::writeOptional, ByteBufUtils::writeOrZero, ByteBufUtils::writeMandatory);
    }

    @Test
    public void testUint32() {
        testRead(Uint32.ONE, ByteBufUtils::readUint32, (byte) 0, (byte) 0, (byte) 0, (byte) 1);
        testWrite(Uint32.ONE, 4, ByteBufUtils::writeOptional, ByteBufUtils::writeOrZero, ByteBufUtils::writeMandatory);
    }

    @Test
    public void testUint64() {
        testRead(Uint64.ONE, ByteBufUtils::readUint64,
            (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 1);
        testWrite(Uint64.ONE, 8, ByteBufUtils::writeOptional, ByteBufUtils::writeOrZero, ByteBufUtils::writeMandatory);
    }

    private static <T> void testRead(final T expected, final Function<ByteBuf, T> readFunc, final byte... bytes) {
        final ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        assertEquals(expected, readFunc.apply(buf));
        assertFalse(buf.isReadable());
    }

    private static <T> void testWrite(final T obj, final int size, final WriteNullable<T> writeOptional,
            final WriteNullable<T> writeOrZero, final WriteMandatory<T> writeMandatory) {
        final ByteBuf buf = Unpooled.buffer(size);

        writeOptional.accept(buf, null);
        assertEquals(0, buf.writerIndex());
        writeOptional.accept(buf, obj);
        assertEquals(size, buf.writerIndex());

        writeOrZero.accept(buf, null);
        assertEquals(2 * size, buf.writerIndex());
        writeOrZero.accept(buf, obj);
        assertEquals(3 * size, buf.writerIndex());

        buf.clear();
        try {
            writeMandatory.accept(buf, null, "name");
            fail("IAE should have been thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("name is mandatory", e.getMessage());
        }
        assertEquals(0, buf.writerIndex());

        writeMandatory.accept(buf, obj, null);
        assertEquals(size, buf.writerIndex());
    }

    @FunctionalInterface
    interface WriteNullable<T> extends BiConsumer<ByteBuf, T> {

    }

    @FunctionalInterface
    interface WriteMandatory<T> {
        void accept(ByteBuf buf, T value, String name);
    }
}
