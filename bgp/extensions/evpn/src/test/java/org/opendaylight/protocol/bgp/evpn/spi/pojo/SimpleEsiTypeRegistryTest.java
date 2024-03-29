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
import static org.junit.Assert.assertThrows;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.RouterIdParserTest.ROUTE_ID_CASE;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.RouterIdParserTest;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.SimpleEsiTypeRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.routes.evpn.routes.EvpnRoute;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;

public class SimpleEsiTypeRegistryTest {
    private static final class NotRegistered implements Esi {
        @Override
        public Class<NotRegistered> implementedInterface() {
            return NotRegistered.class;
        }
    }

    private static final int ESI_TYPE_LENGTH = 10;

    @Test
    public void registryTest() {
        final ByteBuf buff = Unpooled.buffer(ESI_TYPE_LENGTH);

        final SimpleEsiTypeRegistry reg = SimpleEsiTypeRegistry.getInstance();
        SimpleEsiTypeRegistry.getInstance().serializeEsi(ROUTE_ID_CASE, buff);
        assertArrayEquals(RouterIdParserTest.RESULT, ByteArray.getAllBytes(buff));
        assertEquals(ROUTE_ID_CASE, reg.parseEsiModel(RouterIdParserTest.createRouterIdCase()));
        assertEquals(RouterIdParserTest.ROUTE_ID_CASE, reg.parseEsi(Unpooled.wrappedBuffer(buff)));
    }

    @Test
    public void registryParseTest() {
        assertThrows(IllegalArgumentException.class, () -> SimpleEsiTypeRegistry.getInstance().parseEsi(null));
    }

    @Test
    public void registryNullTest() {
        final ByteBuf body = Unpooled.buffer();
        SimpleEsiTypeRegistry.getInstance().serializeEsi(new NotRegistered(), body);
        assertEquals(0, body.readableBytes());
    }

    @Test
    public void registryNullModelTest() {
        assertThrows(IllegalArgumentException.class,
            () -> SimpleEsiTypeRegistry.getInstance().parseEsiModel(ImmutableNodes.newChoiceBuilder()
                .withNodeIdentifier(new NodeIdentifier(QName.create(EvpnRoute.QNAME, "no-register")))
                .build()));
    }

    @Test
    public void registryEmptyModelTest() {
        assertThrows(IllegalArgumentException.class, () -> SimpleEsiTypeRegistry.getInstance().parseEsiModel(null));
    }
}
