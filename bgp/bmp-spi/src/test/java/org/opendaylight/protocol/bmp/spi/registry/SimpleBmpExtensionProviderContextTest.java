/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.spi.registry;

import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.CountTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.description.tlv.DescriptionTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.reason.tlv.ReasonTlv;

public class SimpleBmpExtensionProviderContextTest {

    private static final SimpleBmpMessageRegistry MESSAGE_REGISTRY = new SimpleBmpMessageRegistry();
    private static final SimpleBmpExtensionProviderContext CONTEXT = new SimpleBmpExtensionProviderContext();
    private static final int TEST_TYPE = 1;
    @Mock BmpTlvParser tlvParser;
    @Mock BmpTlvSerializer tlvSerializer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRegisterBmpMessageParser() {
        assertNotNull(CONTEXT.registerBmpMessageParser(TEST_TYPE, MESSAGE_REGISTRY));
    }

    @Test
    public void testRegisterBmpMessageSerializer() {
        assertNotNull(CONTEXT.registerBmpMessageSerializer(Keepalive.class, MESSAGE_REGISTRY));
    }

    @Test
    public void testGetBmpMessageRegistry() {
        assertNotNull(CONTEXT.getBmpMessageRegistry());
    }

    @Test
    public void testRegisterBmpStatisticsTlvParser() {
        assertNotNull(CONTEXT.registerBmpStatisticsTlvParser(TEST_TYPE, this.tlvParser));
    }

    @Test
    public void testRegisterBmpStatisticsTlvSerializer() {
        assertNotNull(CONTEXT.registerBmpStatisticsTlvSerializer(CountTlv.class, this.tlvSerializer));
    }

    @Test
    public void testRegisterBmpInitiationTlvParser() {
        assertNotNull(CONTEXT.registerBmpInitiationTlvParser(TEST_TYPE, this.tlvParser));
    }

    @Test
    public void testRegisterBmpInitiationTlvSerializer() {
        assertNotNull(CONTEXT.registerBmpInitiationTlvSerializer(DescriptionTlv.class, this.tlvSerializer));
    }

    @Test
    public void testRegisterBmpTerminationTlvParser() {
        assertNotNull(CONTEXT.registerBmpTerminationTlvParser(TEST_TYPE, this.tlvParser));
    }

    @Test
    public void testRegisterBmpTerminationTlvSerializer() {
        assertNotNull(CONTEXT.registerBmpTerminationTlvSerializer(ReasonTlv.class, this.tlvSerializer));
    }

    @Test
    public void tetsGetBmpStatisticsTlvRegistry() {
        assertNotNull(CONTEXT.getBmpStatisticsTlvRegistry());
    }

    @Test
    public void testGetBmpInitiationTlvRegistry() {
        assertNotNull(CONTEXT.getBmpInitiationTlvRegistry());
    }

    @Test
    public void testGetBmpTerminationTlvRegistry() {
        assertNotNull(CONTEXT.getBmpTerminationTlvRegistry());
    }
}