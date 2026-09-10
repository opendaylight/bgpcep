/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.nlri;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.VALUE_SIZE;
import static org.opendaylight.protocol.bgp.evpn.impl.EvpnTestUtil.createContBuilder;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.EthADRParserTest.ETHERNET_AD_ROUTE_CASE;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.EthADRParserTest.ROUDE_DISTIN;
import static org.opendaylight.protocol.bgp.evpn.impl.nlri.EthADRParserTest.createEthADRModel;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.Evpn;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev200120.evpn.EvpnChoice;
import org.opendaylight.yangtools.binding.CaseObject;
import org.opendaylight.yangtools.binding.lib.AbstractAugmentable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;

class SimpleEvpnNlriRegistryTest {
    private static final class NotRegistered extends AbstractAugmentable<NotRegistered>
            implements CaseObject<Evpn, EvpnChoice, NotRegistered>, EvpnChoice {
        @Override
        public Class<NotRegistered> implementedInterface() {
            return NotRegistered.class;
        }

        @Override
        public int javaHC() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean javaEQ(final NotRegistered obj) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String javaTS() {
            throw new UnsupportedOperationException();
        }
    }

    public static final NodeIdentifier EVPN_NID = new NodeIdentifier(EvpnChoice.QNAME);

    @Test
    void registryTest() {
        final var buff = SimpleEvpnNlriRegistry.getInstance().serializeEvpn(ETHERNET_AD_ROUTE_CASE,
                Unpooled.wrappedBuffer(ROUDE_DISTIN));
        assertArrayEquals(EthADRParserTest.RESULT, ByteArray.getAllBytes(buff));
        final var resultModel = SimpleEvpnNlriRegistry.getInstance().serializeEvpnModel(createEthADRModel());
        assertEquals(ETHERNET_AD_ROUTE_CASE, resultModel);
        final var type = NlriType.forValue(buff.readUnsignedByte());
        assertNotNull(type);
        buff.skipBytes(VALUE_SIZE); // length + RD
        assertEquals(ETHERNET_AD_ROUTE_CASE, SimpleEvpnNlriRegistry.getInstance().parseEvpn(type, buff));
    }

    @Test
    void registryParseTest() {
        assertThrows(IllegalArgumentException.class,
            () -> SimpleEvpnNlriRegistry.getInstance().parseEvpn(NlriType.EthADDisc, null));
    }

    @Test
    void registryNullTest() {
        final var body = Unpooled.buffer();
        SimpleEvpnNlriRegistry.getInstance().serializeEvpn(new NotRegistered(), body);
        assertEquals(0, body.readableBytes());
    }

    @Test
    void registryNullModelTest() {
        assertNull(SimpleEvpnNlriRegistry.getInstance().serializeEvpnModel(ImmutableNodes.newChoiceBuilder()
            .withNodeIdentifier(EVPN_NID)
            .addChild(createContBuilder(new NodeIdentifier(QName.create(EvpnChoice.QNAME, "test").intern())).build())
            .build()));
    }
}
