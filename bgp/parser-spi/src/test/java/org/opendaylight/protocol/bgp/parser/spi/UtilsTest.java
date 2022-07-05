/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.doReturn;

import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class UtilsTest {

    @Mock private AddressFamilyRegistry afiReg;
    @Mock private SubsequentAddressFamilyRegistry safiReg;

    @Before
    public void setUp() {
        doReturn(1).when(this.afiReg).numberForClass(Ipv4AddressFamily.VALUE);
        doReturn(Ipv4AddressFamily.class).when(this.afiReg).classForFamily(1);
        doReturn(null).when(this.afiReg).classForFamily(2);

        doReturn(1).when(this.safiReg).numberForClass(UnicastSubsequentAddressFamily.VALUE);
        doReturn(UnicastSubsequentAddressFamily.class).when(this.safiReg).classForFamily(1);
        doReturn(null).when(this.safiReg).classForFamily(3);
    }

    @Test
    public void testCapabilityUtil() {
        final byte[] result = new byte[] { 1, 2, 4, 8 };
        final ByteBuf aggregator = Unpooled.buffer();
        CapabilityUtil.formatCapability(1, Unpooled.wrappedBuffer(new byte[] { 4, 8 }),aggregator);
        assertArrayEquals(result, ByteArray.getAllBytes(aggregator));
    }

    @Test
    public void testMessageUtil() {
        final byte[] result = new byte[] { UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE,
            UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE,
            UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE,
            UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE,
            UnsignedBytes.MAX_VALUE, 0, 23, 3, 32, 5, 14, 21 };
        final ByteBuf formattedMessage = Unpooled.buffer();
        MessageUtil.formatMessage(3, Unpooled.wrappedBuffer(new byte[] { 32, 5, 14, 21 }), formattedMessage);
        assertArrayEquals(result, ByteArray.getAllBytes(formattedMessage));
    }

    @Test
    public void testParameterUtil() throws ParameterLengthOverflowException {
        final byte[] result = new byte[] { 1, 2, 4, 8 };
        final ByteBuf aggregator = Unpooled.buffer();
        ParameterUtil.formatParameter(1, Unpooled.wrappedBuffer(new byte[] { 4, 8 }), aggregator);
        assertArrayEquals(result, ByteArray.getAllBytes(aggregator));
    }

    @Test
    public void testAttributeUtil() {
        final byte[] result = new byte[] { 0x40, 03, 04, 10, 00, 00, 02 };
        final ByteBuf aggregator = Unpooled.buffer();
        AttributeUtil.formatAttribute(64 , 3 , Unpooled.wrappedBuffer(new byte[] { 10, 0, 0, 2 }), aggregator);
        assertArrayEquals(result, ByteArray.getAllBytes(aggregator));
    }

    @Test
    public void testAttributeUtilExtended() {
        final byte[] value = new byte[258];
        Arrays.fill(value, 0, 258, UnsignedBytes.MAX_VALUE);
        final byte[] header = new byte[] { (byte) 0x50, 03, 01, 02 };
        final byte[] result = new byte[262];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(value, 0, result, 4, value.length);
        final ByteBuf aggregator = Unpooled.buffer();
        AttributeUtil.formatAttribute(AttributeUtil.TRANSITIVE , 3 , Unpooled.wrappedBuffer(value), aggregator);
        assertArrayEquals(result, ByteArray.getAllBytes(aggregator));
    }

    @Test
    public void testMultiprotocolCapabilitiesUtil() throws BGPParsingException {
        final byte[] bytes = new byte[] {0, 1, 0, 1};
        final ByteBuf bytesBuf = Unpooled.copiedBuffer(bytes);
        final BgpTableType parsedAfiSafi = MultiprotocolCapabilitiesUtil.parseMPAfiSafi(bytesBuf, this.afiReg,
            this.safiReg).get();
        assertEquals(Ipv4AddressFamily.class, parsedAfiSafi.getAfi());
        assertEquals(UnicastSubsequentAddressFamily.class, parsedAfiSafi.getSafi());

        final ByteBuf serializedAfiSafi = Unpooled.buffer(4);
        MultiprotocolCapabilitiesUtil.serializeMPAfiSafi(this.afiReg, this.safiReg, Ipv4AddressFamily.VALUE,
            UnicastSubsequentAddressFamily.VALUE, serializedAfiSafi);
        assertArrayEquals(bytes, serializedAfiSafi.array());
    }

    @Test
    public void testUnsupportedAfi() {
        final byte[] bytes = new byte[] {0, 2, 0, 1};
        final ByteBuf bytesBuf = Unpooled.copiedBuffer(bytes);
        final Optional<BgpTableType> parsedAfiSafi = MultiprotocolCapabilitiesUtil.parseMPAfiSafi(bytesBuf, this.afiReg,
            this.safiReg);
        assertFalse(parsedAfiSafi.isPresent());
    }

    @Test
    public void testUnsupportedSafi() {
        final byte[] bytes = new byte[] {0, 1, 0, 3};
        final ByteBuf bytesBuf = Unpooled.copiedBuffer(bytes);
        final Optional<BgpTableType> parsedAfiSafi = MultiprotocolCapabilitiesUtil.parseMPAfiSafi(bytesBuf, this.afiReg,
            this.safiReg);
        assertFalse(parsedAfiSafi.isPresent());
    }

    @Test(expected = ParameterLengthOverflowException.class)
    public void testFormatParameterOverflow() throws ParameterLengthOverflowException {
        ParameterUtil.formatParameter(2, Unpooled.buffer().writeZero(256), Unpooled.buffer());
    }

    @Test
    public void testFormatParameter() throws ParameterLengthOverflowException {
        final ByteBuf output = Unpooled.buffer();
        ParameterUtil.formatParameter(2, Unpooled.buffer().writeZero(255), output);

        assertEquals(257, output.readableBytes());
        assertEquals(2, output.readUnsignedByte());
        assertEquals(255, output.readUnsignedByte());
    }

    @Test
    public void testFormatExtendedParameter() {
        final ByteBuf output = Unpooled.buffer();
        ParameterUtil.formatExtendedParameter(2, Unpooled.buffer().writeZero(256), output);

        assertEquals(259, output.readableBytes());
        assertEquals(2, output.readUnsignedByte());
        assertEquals(256, output.readUnsignedShort());
    }
}
