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
import static org.junit.Assert.assertNull;
import static org.opendaylight.protocol.bgp.evpn.impl.esi.types.RouterIdParserTest.ROUTE_ID_CASE;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.ESIActivator;
import org.opendaylight.protocol.bgp.evpn.impl.esi.types.RouterIdParserTest;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.Esi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.evpn.routes.EvpnRoute;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;

public class SimpleEsiTypeRegistryTest {
    private static final int ESI_TYPE_LENGTH = 10;

    private class notRegistered implements Esi {
        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return notRegistered.class;
        }
    }

    @Before
    public void setUp() {
        ESIActivator.registerEsiTypeParsers(new ArrayList<>());
    }

    @Test
    public void registryTest() {
        final ByteBuf buff = Unpooled.buffer(ESI_TYPE_LENGTH);
        SimpleEsiTypeRegistry.getInstance().serializeEsi(ROUTE_ID_CASE, buff);
        assertArrayEquals(RouterIdParserTest.RESULT, ByteArray.getAllBytes(buff));
        assertEquals(ROUTE_ID_CASE, SimpleEsiTypeRegistry.getInstance().parseEsiModel(RouterIdParserTest.createRouterIdCase()));
        assertEquals(RouterIdParserTest.ROUTE_ID_CASE, SimpleEsiTypeRegistry.getInstance().parseEsi(Unpooled.wrappedBuffer(buff)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void registryParseTest() {
        SimpleEsiTypeRegistry.getInstance().parseEsi(null);
    }

    @Test
    public void registryNullTest() {
        final ByteBuf body = Unpooled.buffer();
        SimpleEsiTypeRegistry.getInstance().serializeEsi(new notRegistered(), body);
        assertEquals(0, body.readableBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void registryNullModelTest() {
        final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ChoiceNode> noRegister = Builders.choiceBuilder();
        noRegister.withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(QName.create(EvpnRoute.QNAME, "no-register").intern()));
        assertNull(SimpleEsiTypeRegistry.getInstance().parseEsiModel(noRegister.build()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void registryEmptyModelTest() {
        assertNull(SimpleEsiTypeRegistry.getInstance().parseEsiModel(null));
    }
}