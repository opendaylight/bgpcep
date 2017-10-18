/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static org.junit.Assert.assertArrayEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.UnrecognizedAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.UnrecognizedAttributesBuilder;

public class UnrecognizedAttributesSerializerTest {

    @Test
    public void testUnrecognizedAttributesSerializer() {
        final byte[] unrecognizedValue1 = { (byte)0xd3, 0x5d, 0x35, (byte)0xd3, 0x5d, 0x35, (byte)0xd3, 0x5d, 0x35, (byte)0xd3, 0x5d, 0x35 };
        final byte[] unrecognizedValue2 = { (byte)0xd3, 0x5d, 0x35, (byte)0xd3, 0x5d, 0x35, (byte)0xd7, 0x5d, 0x75, (byte)0xd7, 0x5d, 0x75 };
        final byte[] unrecognizedBytes = { (byte)0xe0, 0x65, 0x0c, (byte)0xd3, 0x5d, 0x35, (byte)0xd3, 0x5d, 0x35, (byte)0xd3, 0x5d, 0x35, (byte)0xd3, 0x5d, 0x35,
                                           (byte)0xe0, 0x66, 0x0c, (byte)0xd3, 0x5d, 0x35, (byte)0xd3, 0x5d, 0x35, (byte)0xd7, 0x5d, 0x75, (byte)0xd7, 0x5d, 0x75 };
        final List<UnrecognizedAttributes> unrecognizedAttrs = new ArrayList<>();
        final UnrecognizedAttributes unrecognizedAttribute1 = new UnrecognizedAttributesBuilder().setPartial(true).setTransitive(true).setType((short) 101).setValue(unrecognizedValue1).build();
        unrecognizedAttrs.add(unrecognizedAttribute1);
        final UnrecognizedAttributes unrecognizedAttribute2 = new UnrecognizedAttributesBuilder().setPartial(true).setTransitive(true).setType((short) 102).setValue(unrecognizedValue2).build();
        unrecognizedAttrs.add(unrecognizedAttribute2);
        final Attributes attrs = new AttributesBuilder().setUnrecognizedAttributes(unrecognizedAttrs).build();

        final ByteBuf buffer = Unpooled.buffer();
        ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry().serializeAttribute(attrs, buffer);
        assertArrayEquals(unrecognizedBytes, ByteArray.readAllBytes(buffer));
    }
}
