/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.ObjectHeaderImpl;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.endpoints.address.family.Ipv4Case;

class PCCEndPointIpv4ObjectParserTest {
    private static final String IP1 = "1.2.3.4";
    private static final String IP2 = "1.2.3.5";

    @Test
    void testParseEmptyObject() {
        final var header = new ObjectHeaderImpl(false, false);
        final var bytes = Unpooled.buffer();
        bytes.writeByte(4);
        final var ex = assertThrows(PCEPDeserializerException.class,
            () -> new PCCEndPointIpv4ObjectParser().parseObject(header, bytes));
        assertEquals("Wrong length of array of bytes.", ex.getMessage());
    }

    @Test
    void testParseObject() throws Exception {
        final var header = new ObjectHeaderImpl(false, false);
        final var bytes = Unpooled.buffer();
        bytes.writeBytes(Ipv4Util.bytesForAddress(new Ipv4AddressNoZone(IP1)));
        bytes.writeBytes(Ipv4Util.bytesForAddress(new Ipv4AddressNoZone(IP2)));
        final var output = assertInstanceOf(EndpointsObj.class,
            new PCCEndPointIpv4ObjectParser().parseObject(header, bytes));

        final var ipv4 = assertInstanceOf(Ipv4Case.class, output.getAddressFamily()).getIpv4();
        assertEquals(IP1, ipv4.getSourceIpv4Address().getValue());
        assertEquals(IP2, ipv4.getDestinationIpv4Address().getValue());
        assertFalse(output.getIgnore());
        assertFalse(output.getProcessingRule());
    }

    @Test
    void testNullBytes() {
        final var header = new ObjectHeaderImpl(false, false);
        final var ex = assertThrows(IllegalArgumentException.class,
            () -> new PCCEndPointIpv4ObjectParser().parseObject(header, null));
        assertEquals("Array of bytes is mandatory. Can't be null or empty.", ex.getMessage());
    }

    @Test
    void testEmptyBytes() {
        final var header = new ObjectHeaderImpl(false, false);
        final var ex = assertThrows(IllegalArgumentException.class,
            () -> new PCCEndPointIpv4ObjectParser().parseObject(header, Unpooled.EMPTY_BUFFER));
        assertEquals("Array of bytes is mandatory. Can't be null or empty.", ex.getMessage());
    }
}
