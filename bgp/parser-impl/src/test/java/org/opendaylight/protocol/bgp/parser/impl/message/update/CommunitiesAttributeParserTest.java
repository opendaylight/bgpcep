/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.NoopReferenceCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Communities;

public class CommunitiesAttributeParserTest {
    private static final byte[] COMMUNITIES_BYTES = {
        (byte) 0xC0, (byte) 0x08, (byte) 0x18,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x01,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x02,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x03,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x10,
        (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x06,
        (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x07
    };

    @Test
    public void testCommunitiesAttributeParser() throws Exception {
        final List<Communities> comms = new ArrayList<>();
        comms.add((Communities) CommunityUtil.NO_EXPORT);
        comms.add((Communities) CommunityUtil.NO_ADVERTISE);
        comms.add((Communities) CommunityUtil.NO_EXPORT_SUBCONFED);
        comms.add((Communities) CommunityUtil.create(NoopReferenceCache.getInstance(), 0xFFFF, 0xFF10));
        comms.add((Communities) CommunityUtil.LLGR_STALE);
        comms.add((Communities) CommunityUtil.NO_LLGR);

        final AttributesBuilder paBuilder = new AttributesBuilder();
        paBuilder.setCommunities(comms);

        final ByteBuf actual = Unpooled.buffer();
        ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry()
            .serializeAttribute(paBuilder.build(), actual);
        assertArrayEquals(COMMUNITIES_BYTES, ByteArray.getAllBytes(actual));
        final Attributes attributeOut = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance()
            .getAttributeRegistry().parseAttributes(actual, null).getAttributes();
        assertEquals(comms, attributeOut.getCommunities());
    }

    @Test
    public void testParseEmptyListAttribute() {
        final List<Communities> comms = new ArrayList<>();
        final ByteBuf actual = Unpooled.buffer();
        ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry()
            .serializeAttribute(new AttributesBuilder().setCommunities(comms).build(), actual);
        assertEquals(Unpooled.buffer(), actual);
    }

    @Test
    public void testParseEmptyAttribute() {
        final ByteBuf actual = Unpooled.buffer();
        ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry()
            .serializeAttribute(new AttributesBuilder().build(), actual);
        assertEquals(Unpooled.buffer(), actual);
    }
}