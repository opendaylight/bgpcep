/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled.unicast;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.Srgb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.originator.srgb.tlv.SrgbValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.originator.srgb.tlv.SrgbValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.LuLabelIndexTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.LuLabelIndexTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.LuOriginatorSrgbTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev171207.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.LuOriginatorSrgbTlvBuilder;

public final class BgpPrefixSidTlvsTest {

    @Test
    public void testLabelIndexParser() {
        final LabelIndexTlvParser parser = new LabelIndexTlvParser();
        final LuLabelIndexTlv tlv = new LuLabelIndexTlvBuilder().setLabelIndexTlv(333L).build();
        final ByteBuf serialized = Unpooled.buffer(7);
        final byte[] expected = new byte[] {0, 0, 0, 0, 0, 1, (byte)0x4d};
        parser.serializeBgpPrefixSidTlv(tlv, serialized);
        assertArrayEquals(expected, serialized.array());

        assertEquals(tlv.getLabelIndexTlv(), parser.parseBgpPrefixSidTlv(serialized).getLabelIndexTlv());

        assertEquals(1, parser.getType());
    }

    @Test
    public void testOriginatorParser() {
        final OriginatorSrgbTlvParser parser = new OriginatorSrgbTlvParser();
        final List<SrgbValue> list = new ArrayList<>();

        final Srgb srgb1 = new Srgb(1L);
        final Srgb srgb2 = new Srgb(2L);
        list.add(new SrgbValueBuilder().setBase(srgb1).setRange(srgb2).build());
        list.add(new SrgbValueBuilder().setBase(srgb2).setRange(srgb1).build());

        final LuOriginatorSrgbTlv tlv = new LuOriginatorSrgbTlvBuilder().setSrgbValue(list).build();
        final ByteBuf serialized = Unpooled.buffer(14);
        parser.serializeBgpPrefixSidTlv(tlv, serialized);
        final byte[] expected = new byte[] {0, 0, 0, 0, 1, 0, 0, 2, 0, 0, 2, 0, 0, 1};
        assertArrayEquals(expected, serialized.array());

        final LuOriginatorSrgbTlv parsed = parser.parseBgpPrefixSidTlv(serialized);
        assertEquals(tlv.getSrgbValue().size(), parsed.getSrgbValue().size());
        assertTrue(tlv.getSrgbValue().get(0).getBase().equals(srgb1));
        assertTrue(tlv.getSrgbValue().get(0).getRange().equals(srgb2));
        assertTrue(tlv.getSrgbValue().get(1).getBase().equals(srgb2));
        assertTrue(tlv.getSrgbValue().get(1).getRange().equals(srgb1));

        assertEquals(3, parser.getType());
    }
}
