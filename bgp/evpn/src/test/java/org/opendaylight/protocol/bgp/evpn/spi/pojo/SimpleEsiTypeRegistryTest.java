/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.spi.pojo;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.RouterIdParserTest.ROUTE_ID_CASE;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.ESIActivator;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.RouterIdParserTest;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.esi.esi.RouterIdGeneratedCase;

public class SimpleEsiTypeRegistryTest {
    private static final int ESI_TYPE_LENGTH = 10;

    @Before
    public void setUp() {
        ESIActivator.registerEsiTypeParsers(new ArrayList<>());
    }

    @Test
    public void registryTest() {
        final ByteBuf buff = Unpooled.buffer(ESI_TYPE_LENGTH);
        SimpleEsiTypeRegistry.getInstance().serializeEsi(ROUTE_ID_CASE, buff);
        assertArrayEquals(RouterIdParserTest.RESULT, ByteArray.getAllBytes(buff));
        assertEquals(ROUTE_ID_CASE, SimpleEsiTypeRegistry.getInstance().serializeEsiModel(RouterIdGeneratedCase.class, RouterIdParserTest.createRouteContainer()));
        assertEquals(RouterIdParserTest.ROUTE_ID_CASE, SimpleEsiTypeRegistry.getInstance().parseEsi(Unpooled.wrappedBuffer(buff)));
    }
}