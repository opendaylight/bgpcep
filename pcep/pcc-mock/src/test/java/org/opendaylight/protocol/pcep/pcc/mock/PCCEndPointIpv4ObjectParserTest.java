/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.pcep.spi.ObjectHeaderImpl;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObj;

public class PCCEndPointIpv4ObjectParserTest {

    private static final String IP1 = "1.2.3.4";
    private static final String IP2 = "1.2.3.5";

    @Test(expected=PCEPDeserializerException.class)
    public void testParseEmptyObject() throws PCEPDeserializerException {
        final ObjectHeader header = new ObjectHeaderImpl(false, false);
        final ByteBuf bytes = Unpooled.buffer();
        bytes.writeByte(4);
        new PCCEndPointIpv4ObjectParser().parseObject(header, bytes);
    }

    @Test
    public void testParseObject() throws PCEPDeserializerException {
        final ObjectHeader header = new ObjectHeaderImpl(false, false);
        final ByteBuf bytes = Unpooled.buffer();
        bytes.writeBytes(Ipv4Util.bytesForAddress(new Ipv4Address(IP1)));
        bytes.writeBytes(Ipv4Util.bytesForAddress(new Ipv4Address(IP2)));
        final EndpointsObj output = (EndpointsObj) new PCCEndPointIpv4ObjectParser().parseObject(header, bytes);

        assertEquals(IP1, ((Ipv4Case) output.getAddressFamily()).getIpv4().getSourceIpv4Address().getValue());
        assertEquals(IP2, ((Ipv4Case) output.getAddressFamily()).getIpv4().getDestinationIpv4Address().getValue());
        assertFalse(output.isIgnore());
        assertFalse(output.isProcessingRule());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNullBytes() throws PCEPDeserializerException {
        final ObjectHeader header = new ObjectHeaderImpl(false, false);
        final ByteBuf bytes = null;
        new PCCEndPointIpv4ObjectParser().parseObject(header, bytes);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testEmptyBytes() throws PCEPDeserializerException {
        final ObjectHeader header = new ObjectHeaderImpl(false, false);
        final ByteBuf bytes = Unpooled.buffer();
        new PCCEndPointIpv4ObjectParser().parseObject(header, bytes);
    }
}
