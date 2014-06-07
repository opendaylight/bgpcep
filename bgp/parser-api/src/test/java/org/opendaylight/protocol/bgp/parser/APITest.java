/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.As4BytesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.as4.bytes._case.As4BytesCapabilityBuilder;

public class APITest {

    @Test
    public void testDocumentedException() {
        final BGPDocumentedException de = new BGPDocumentedException("Some message", BGPError.BAD_BGP_ID);
        assertEquals("Some message", de.getMessage());
        assertEquals(BGPError.BAD_BGP_ID, de.getError());
        assertNull(de.getData());

        final BGPDocumentedException doc = BGPDocumentedException.badMessageLength("Wrong length", 5000);
        assertEquals(5000, ByteArray.bytesToInt(doc.getData()));
    }

    @Test
    public void testParsingException() {
        final BGPParsingException de = new BGPParsingException("Some message");
        assertEquals("Some message", de.getMessage());
    }

    @Test
    public void testBGPError() {
        assertEquals(BGPError.BAD_MSG_TYPE, BGPError.forValue(1, 3));
    }

    @Test
    public void testTerminationReason() {
        assertEquals(BGPError.BAD_PEER_AS.toString(), new BGPTerminationReason(BGPError.BAD_PEER_AS).getErrorMessage());
    }

    @Test
    public void testAsNumberUtil() {
        final List<BgpParameters> params = new ArrayList<>();
        params.add(new BgpParametersBuilder().setCParameters(
                new As4BytesCaseBuilder().setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(new AsNumber(35L)).build()).build()).build());
        final Open open1 = new OpenBuilder().setBgpParameters(params).build();
        assertEquals(35L, AsNumberUtil.advertizedAsNumber(open1).getValue().longValue());

        final Open open2 = new OpenBuilder().setMyAsNumber(10).build();
        assertEquals(10, AsNumberUtil.advertizedAsNumber(open2).getValue().intValue());
    }
}
