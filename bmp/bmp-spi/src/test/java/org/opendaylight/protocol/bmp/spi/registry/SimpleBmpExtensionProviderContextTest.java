/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.spi.registry;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.CountTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.description.tlv.DescriptionTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.reason.tlv.ReasonTlv;

@ExtendWith(MockitoExtension.class)
class SimpleBmpExtensionProviderContextTest {

    private static final SimpleBmpMessageRegistry MESSAGE_REGISTRY = new SimpleBmpMessageRegistry();
    private static final SimpleBmpExtensionProviderContext CONTEXT = new SimpleBmpExtensionProviderContext();
    private static final int TEST_TYPE = 1;
    @Mock
    private BmpTlvParser tlvParser;
    @Mock
    private BmpTlvSerializer tlvSerializer;

    @Test
    void testRegisterBmpMessageParser() {
        assertNotNull(CONTEXT.registerBmpMessageParser(TEST_TYPE, MESSAGE_REGISTRY));
    }

    @Test
    void testRegisterBmpMessageSerializer() {
        assertNotNull(CONTEXT.registerBmpMessageSerializer(Keepalive.class, MESSAGE_REGISTRY));
    }

    @Test
    void testGetBmpMessageRegistry() {
        assertNotNull(CONTEXT.getBmpMessageRegistry());
    }

    @Test
    void testRegisterBmpStatisticsTlvParser() {
        assertNotNull(CONTEXT.registerBmpStatisticsTlvParser(TEST_TYPE, this.tlvParser));
    }

    @Test
    void testRegisterBmpStatisticsTlvSerializer() {
        assertNotNull(CONTEXT.registerBmpStatisticsTlvSerializer(CountTlv.class, this.tlvSerializer));
    }

    @Test
    void testRegisterBmpInitiationTlvParser() {
        assertNotNull(CONTEXT.registerBmpInitiationTlvParser(TEST_TYPE, this.tlvParser));
    }

    @Test
    void testRegisterBmpInitiationTlvSerializer() {
        assertNotNull(CONTEXT.registerBmpInitiationTlvSerializer(DescriptionTlv.class, this.tlvSerializer));
    }

    @Test
    void testRegisterBmpTerminationTlvParser() {
        assertNotNull(CONTEXT.registerBmpTerminationTlvParser(TEST_TYPE, this.tlvParser));
    }

    @Test
    void testRegisterBmpTerminationTlvSerializer() {
        assertNotNull(CONTEXT.registerBmpTerminationTlvSerializer(ReasonTlv.class, this.tlvSerializer));
    }

    @Test
    void tetsGetBmpStatisticsTlvRegistry() {
        assertNotNull(CONTEXT.getBmpStatisticsTlvRegistry());
    }

    @Test
    void testGetBmpInitiationTlvRegistry() {
        assertNotNull(CONTEXT.getBmpInitiationTlvRegistry());
    }

    @Test
    void testGetBmpTerminationTlvRegistry() {
        assertNotNull(CONTEXT.getBmpTerminationTlvRegistry());
    }
}
