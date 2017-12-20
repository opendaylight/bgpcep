/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.auto.bandwidth.extension;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.pcep.spi.ObjectHeaderImpl;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev171025.bandwidth.usage.object.BandwidthUsage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev171025.bandwidth.usage.object.BandwidthUsageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;

public class PCEPBandwidthUsageObjectCodecTest {

    private static final byte[] BW_BYTES = new byte[]{0x05, 0x50, 0x00, 0x0C, 0x00, 0x00, 0x10, 0x00,
        0x00, 0x00, 0x40, 0x00};

    @Test
    public void testCodec() throws PCEPDeserializerException {
        final BandwidthUsageObjectCodec codec = new BandwidthUsageObjectCodec(5);
        assertEquals(5, codec.getType());

        final BandwidthUsageBuilder builder = new BandwidthUsageBuilder();
        builder.setBwSample(Lists.newArrayList(new Bandwidth(new byte[]{0x00, 0x00, 0x10, 0x00}),
                new Bandwidth(new byte[]{0x00, 0x00, 0x40, 0x00})));
        builder.setIgnore(false);
        builder.setProcessingRule(false);
        final BandwidthUsage parsedObject = codec.parseObject(new ObjectHeaderImpl(false, false),
                Unpooled.wrappedBuffer(BW_BYTES, 4, BW_BYTES.length - 4));
        assertEquals(builder.build(), parsedObject);

        final ByteBuf buffer = Unpooled.buffer(BW_BYTES.length);
        codec.serializeObject(builder.build(), buffer);
        assertArrayEquals(BW_BYTES, ByteArray.getAllBytes(buffer));
    }

}
