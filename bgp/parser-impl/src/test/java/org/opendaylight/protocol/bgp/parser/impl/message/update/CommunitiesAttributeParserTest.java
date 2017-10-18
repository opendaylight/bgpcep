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

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.NoopReferenceCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.Communities;

public class CommunitiesAttributeParserTest {

    private static final byte[] CommunitiesBytes = {(byte) 0xC0, (byte) 0x08, (byte) 0x10,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x1,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x2,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x3,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x10};

    @Test
    public void testCommunitiesAttributeParser() throws Exception {
        final List<Communities> comms = Lists.newArrayList();
        comms.add((Communities) CommunityUtil.NO_EXPORT);
        comms.add((Communities) CommunityUtil.NO_ADVERTISE);
        comms.add((Communities) CommunityUtil.NO_EXPORT_SUBCONFED);
        comms.add((Communities) CommunityUtil.create(NoopReferenceCache.getInstance(), 0xFFFF, 0xFF10));

        final AttributesBuilder paBuilder = new AttributesBuilder();
        paBuilder.setCommunities(comms);

        final ByteBuf actual = Unpooled.buffer();
        ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry()
            .serializeAttribute(paBuilder.build(), actual);
        assertArrayEquals(CommunitiesBytes, ByteArray.getAllBytes(actual));
        final Attributes attributeOut = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance()
            .getAttributeRegistry().parseAttributes(actual, null);
        assertEquals(comms, attributeOut.getCommunities());
    }

    @Test
    public void testParseEmptyListAttribute() {
        final List<Communities> comms = Lists.newArrayList();
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