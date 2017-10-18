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

import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class UtilsTest {

    @Mock private AddressFamilyRegistry afiReg;
    @Mock private SubsequentAddressFamilyRegistry safiReg;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn(1).when(this.afiReg).numberForClass(Ipv4AddressFamily.class);
        Mockito.doReturn(Ipv4AddressFamily.class).when(this.afiReg).classForFamily(1);
        Mockito.doReturn(null).when(this.afiReg).classForFamily(2);

        Mockito.doReturn(1).when(this.safiReg).numberForClass(UnicastSubsequentAddressFamily.class);
        Mockito.doReturn(UnicastSubsequentAddressFamily.class).when(this.safiReg).classForFamily(1);
        Mockito.doReturn(null).when(this.safiReg).classForFamily(3);
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
            UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE,
            UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE,
            UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, UnsignedBytes.MAX_VALUE, 0, 23, 3, 32, 5, 14, 21 };
        final ByteBuf formattedMessage = Unpooled.buffer();
        MessageUtil.formatMessage(3, Unpooled.wrappedBuffer(new byte[] { 32, 5, 14, 21 }), formattedMessage);
        assertArrayEquals(result, ByteArray.getAllBytes(formattedMessage));
    }

    @Test
    public void testParameterUtil() {
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
        final BgpTableType parsedAfiSafi = MultiprotocolCapabilitiesUtil.parseMPAfiSafi(bytesBuf, this.afiReg, this.safiReg).get();
        assertEquals(Ipv4AddressFamily.class, parsedAfiSafi.getAfi());
        assertEquals(UnicastSubsequentAddressFamily.class, parsedAfiSafi.getSafi());

        final ByteBuf serializedAfiSafi = Unpooled.buffer(4);
        MultiprotocolCapabilitiesUtil.serializeMPAfiSafi(this.afiReg, this.safiReg, Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class, serializedAfiSafi);
        assertArrayEquals(bytes, serializedAfiSafi.array());
    }

    @Test
    public void testUnsupportedAfi() {
        final byte[] bytes = new byte[] {0, 2, 0, 1};
        final ByteBuf bytesBuf = Unpooled.copiedBuffer(bytes);
        final Optional<BgpTableType> parsedAfiSafi = MultiprotocolCapabilitiesUtil.parseMPAfiSafi(bytesBuf, this.afiReg, this.safiReg);
        Assert.assertFalse(parsedAfiSafi.isPresent());
    }

    @Test
    public void testUnsupportedSafi() {
        final byte[] bytes = new byte[] {0, 1, 0, 3};
        final ByteBuf bytesBuf = Unpooled.copiedBuffer(bytes);
        final Optional<BgpTableType> parsedAfiSafi = MultiprotocolCapabilitiesUtil.parseMPAfiSafi(bytesBuf, this.afiReg, this.safiReg);
        Assert.assertFalse(parsedAfiSafi.isPresent());
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testAttributeUtilPrivateConstructor() throws Throwable {
        final Constructor<AttributeUtil> c = AttributeUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testCapabilityUtilPrivateConstructor() throws Throwable {
        final Constructor<CapabilityUtil> c = CapabilityUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testMessageUtilPrivateConstructor() throws Throwable {
        final Constructor<MessageUtil> c = MessageUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testParameterUtilPrivateConstructor() throws Throwable {
        final Constructor<ParameterUtil> c = ParameterUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
