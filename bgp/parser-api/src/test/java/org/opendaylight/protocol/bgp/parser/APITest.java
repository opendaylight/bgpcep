/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.MultiprotocolCapabilityBuilder;

public class APITest {

    @Test
    public void testDocumentedException() {
        final BGPDocumentedException de = new BGPDocumentedException("Some message", BGPError.BAD_BGP_ID);
        assertEquals("Some message", de.getMessage());
        assertEquals(BGPError.BAD_BGP_ID, de.getError());
        assertEquals(0, de.getData().length);

        final BGPDocumentedException doc = BGPDocumentedException.badMessageLength("Wrong length", 5000);
        assertEquals(5000, ByteArray.bytesToInt(doc.getData()));
    }

    @Test
    public void testParsingException() {
        final BGPParsingException de = new BGPParsingException("Some message");
        assertEquals("Some message", de.getMessage());

        final BGPParsingException de1 = new BGPParsingException("Some message", new IllegalArgumentException("text"));
        assertEquals("text", de1.getCause().getMessage());
    }

    @Test
    public void testBGPError() {
        assertEquals(BGPError.BAD_MSG_TYPE, BGPError.forValue(1, 3));
    }

    @Test
    public void testAsNumberUtil() {
        final List<BgpParameters> params = new ArrayList<>();
        final List<OptionalCapabilities> capas = new ArrayList<>();
        capas.add(new OptionalCapabilitiesBuilder().setCParameters( new CParametersBuilder().addAugmentation(
            CParameters1.class, new CParameters1Builder().setMultiprotocolCapability( new MultiprotocolCapabilityBuilder()
                .build()).build()).build()).build());
        capas.add(new OptionalCapabilitiesBuilder().setCParameters(
            new CParametersBuilder().setAs4BytesCapability( new As4BytesCapabilityBuilder().setAsNumber(
                new AsNumber(35L)).build()).build()).build());
        params.add(new BgpParametersBuilder().setOptionalCapabilities(capas).build());
        final Open open1 = new OpenBuilder().setBgpParameters(params).build();
        assertEquals(35L, AsNumberUtil.advertizedAsNumber(open1).getValue().longValue());

        final Open open2 = new OpenBuilder().setMyAsNumber(10).build();
        assertEquals(10, AsNumberUtil.advertizedAsNumber(open2).getValue().intValue());
    }

    @Test
    public void testBgpExtendedMessageUtil() {
        final List<BgpParameters> params = new ArrayList<>();
        final List<OptionalCapabilities> capas = new ArrayList<>();
        capas.add(new OptionalCapabilitiesBuilder().setCParameters( new CParametersBuilder().addAugmentation(
            CParameters1.class, new CParameters1Builder().setMultiprotocolCapability( new MultiprotocolCapabilityBuilder()
                .build()).build()).build()).build());
        capas.add(new OptionalCapabilitiesBuilder().setCParameters(
                BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY).build());
        params.add(new BgpParametersBuilder().setOptionalCapabilities(capas).build());
        final Open open1 = new OpenBuilder().setBgpParameters(params).build();
        assertEquals(true, BgpExtendedMessageUtil.advertizedBgpExtendedMessageCapability(open1));
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testAsNumberUtilPrivateConstructor() throws Throwable {
        final Constructor<AsNumberUtil> c = AsNumberUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        try {
            c.newInstance();
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
