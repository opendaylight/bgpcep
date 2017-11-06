/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.spi.parser;

import static org.junit.Assert.assertEquals;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.DistinguisherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.DistinguisherBuilder;

public class PeerDistinguisherUtilTest {

    private static final byte[] DISTINGUISHER_TYPE0 = { 0x00, 0x00, 0x30, 0x39, 0x00, 0x09, (byte)0xfb, (byte)0xf1};
    private static final byte[] DISTINGUISHER_TYPE1 = { 0x00, 0x01, 0x7f, (byte)0x9c, (byte)0x97, 0x0b, (byte)0xd4, 0x31};
    private static final byte[] DISTINGUISHER_TYPE2 = { 0x00, 0x02, 0x00, 0x01, (byte)0xe2, 0x40, (byte)0xd4, 0x31};

    private static final int ASN = 12345;
    private static final int ASN_FOUR_BYTES = 123456;
    private static final int ISP_NUMBER = 54321;
    private static final int ISP_NUMBER_FOUR_BYTES = 654321;
    private static final String IPV4_ADDRESS = "127.156.151.11";

    @Test
    public void testPeerDistingisherType0() {
        final DistinguisherBuilder dBuilder = new DistinguisherBuilder();
        dBuilder.setDistinguisherType(DistinguisherType.Type0);
        dBuilder.setDistinguisher(ASN + ":" + ISP_NUMBER_FOUR_BYTES);

        assertEquals(dBuilder.build(), PeerDistinguisherUtil.parsePeerDistingisher(Unpooled.wrappedBuffer(DISTINGUISHER_TYPE0)));

        final ByteBuf buffer = Unpooled.buffer(DISTINGUISHER_TYPE0.length);
        PeerDistinguisherUtil.serializePeerDistinguisher(dBuilder.build(), buffer);
        Assert.assertArrayEquals(DISTINGUISHER_TYPE0, buffer.array());
    }

    @Test
    public void testPeerDistingisherType1() {
        final DistinguisherBuilder dBuilder = new DistinguisherBuilder();
        dBuilder.setDistinguisherType(DistinguisherType.Type1);
        dBuilder.setDistinguisher(IPV4_ADDRESS + ":" + ISP_NUMBER);

        assertEquals(dBuilder.build(), PeerDistinguisherUtil.parsePeerDistingisher(Unpooled.wrappedBuffer(DISTINGUISHER_TYPE1)));

        final ByteBuf buffer = Unpooled.buffer(DISTINGUISHER_TYPE1.length);
        PeerDistinguisherUtil.serializePeerDistinguisher(dBuilder.build(), buffer);
        Assert.assertArrayEquals(DISTINGUISHER_TYPE1, buffer.array());
    }

    @Test
    public void testPeerDistingisherType2() {
        final DistinguisherBuilder dBuilder = new DistinguisherBuilder();
        dBuilder.setDistinguisherType(DistinguisherType.Type2);
        dBuilder.setDistinguisher(ASN_FOUR_BYTES + ":" + ISP_NUMBER);

        assertEquals(dBuilder.build(), PeerDistinguisherUtil.parsePeerDistingisher(Unpooled.wrappedBuffer(DISTINGUISHER_TYPE2)));

        final ByteBuf buffer = Unpooled.buffer(DISTINGUISHER_TYPE2.length);
        PeerDistinguisherUtil.serializePeerDistinguisher(dBuilder.build(), buffer);
        Assert.assertArrayEquals(DISTINGUISHER_TYPE2, buffer.array());
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPeerDistinguisherUtilPrivateConstructor() throws Throwable {
        final Constructor<PeerDistinguisherUtil> c = PeerDistinguisherUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
