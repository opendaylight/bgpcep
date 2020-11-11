/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.VALUE_SIZE;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createContBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.EthADRParserTest.ETHERNET_AD_ROUTE_CASE;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.EthADRParserTest.ROUDE_DISTIN;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.EthADRParserTest.createEthADRModel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.Evpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.EvpnChoice;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;

public final class SimpleEvpnNlriRegistryTest {
    public static final NodeIdentifier EVPN_NID = new NodeIdentifier(EvpnChoice.QNAME);

    private class NotRegistered implements EvpnChoice {
        @Override
        public Class<NotRegistered> implementedInterface() {
            return NotRegistered.class;
        }
    }

    @Test
    public void registryTest() {
        final ByteBuf buff = SimpleEvpnNlriRegistry.getInstance().serializeEvpn(ETHERNET_AD_ROUTE_CASE,
                Unpooled.wrappedBuffer(ROUDE_DISTIN));
        assertArrayEquals(EthADRParserTest.RESULT, ByteArray.getAllBytes(buff));
        final EvpnChoice resultModel = SimpleEvpnNlriRegistry.getInstance().serializeEvpnModel(createEthADRModel());
        assertEquals(ETHERNET_AD_ROUTE_CASE, resultModel);
        final NlriType type = NlriType.forValue(buff.readUnsignedByte());
        buff.skipBytes(VALUE_SIZE); // length + RD
        assertEquals(ETHERNET_AD_ROUTE_CASE, SimpleEvpnNlriRegistry.getInstance().parseEvpn(type, buff));
    }

    @Test(expected = IllegalArgumentException.class)
    public void registryParseTest() {
        SimpleEvpnNlriRegistry.getInstance().parseEvpn(NlriType.EthADDisc, null);
    }

    @Test
    public void registryNullTest() {
        final ByteBuf body = Unpooled.buffer();
        SimpleEvpnNlriRegistry.getInstance().serializeEvpn(new NotRegistered(), body);
        assertEquals(0, body.readableBytes());
    }

    @Test
    public void registryNullModelTest() {
        final DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> choice = Builders.choiceBuilder()
                .withNodeIdentifier(EVPN_NID);
        choice.addChild(createContBuilder(new NodeIdentifier(QName.create(Evpn.QNAME, "test").intern()))
                .build()).build();
        assertNull(SimpleEvpnNlriRegistry.getInstance().serializeEvpnModel(choice.build()));
    }
}
