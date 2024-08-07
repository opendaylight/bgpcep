/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;

import java.util.Map;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AggregatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapability.RestartFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpAggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

public class ComplementaryTest {

    @Test
    public void testBGPParameter() {

        final MultiprotocolCapability cap = new MultiprotocolCapabilityBuilder().setAfi(Ipv6AddressFamily.VALUE)
                .setSafi(UnicastSubsequentAddressFamily.VALUE).build();
        final CParameters tlv1 = new CParametersBuilder().addAugmentation(new CParameters1Builder()
            .setMultiprotocolCapability(cap).build()).build();
        final MultiprotocolCapability cap1 = new MultiprotocolCapabilityBuilder().setAfi(Ipv4AddressFamily.VALUE)
                .setSafi(UnicastSubsequentAddressFamily.VALUE).build();
        final CParameters tlv2 = new CParametersBuilder().addAugmentation(new CParameters1Builder()
            .setMultiprotocolCapability(cap1).build()).build();

        final Map<TablesKey, Tables> tt = BindingMap.of(
            new TablesBuilder().setAfi(Ipv6AddressFamily.VALUE).setSafi(UnicastSubsequentAddressFamily.VALUE).build(),
            new TablesBuilder().setAfi(Ipv4AddressFamily.VALUE).setSafi(UnicastSubsequentAddressFamily.VALUE).build());

        final GracefulRestartCapability tlv3 = new GracefulRestartCapabilityBuilder()
            .setRestartFlags(new RestartFlags(false)).setRestartTime(Uint16.ZERO).setTables(tt).build();

        final CParameters tlv4 = new CParametersBuilder().setAs4BytesCapability(
            new As4BytesCapabilityBuilder().setAsNumber(new AsNumber(Uint32.valueOf(40))).build()).build();

        assertFalse(tlv3.getRestartFlags().getRestartState());

        assertEquals(0, tlv3.getRestartTime().intValue());

        assertNotEquals(tlv1, tlv2);

        assertNotSame(tlv1.hashCode(), tlv3.hashCode());

        assertNotSame(tlv2.toString(), tlv3.toString());

        assertEquals(tt, tlv3.getTables());

        assertEquals(cap.getSafi(), cap1.getSafi());

        assertNotSame(cap.getAfi(), cap1.getAfi());

        assertEquals(40, tlv4.getAs4BytesCapability().getAsNumber().getValue().longValue());

        assertEquals(new CParametersBuilder().setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(
            new AsNumber(Uint32.valueOf(40))).build()).build(), tlv4);
    }

    @Test
    public void testBGPAggregatorImpl() {
        final BgpAggregator ipv4 = new AggregatorBuilder()
                .setAsNumber(new AsNumber(Uint32.valueOf(5524)))
                .setNetworkAddress(new Ipv4AddressNoZone("124.55.42.1"))
                .build();
        final BgpAggregator ipv4i = new AggregatorBuilder()
                .setAsNumber(new AsNumber(Uint32.valueOf(5525)))
                .setNetworkAddress(new Ipv4AddressNoZone("124.55.42.1"))
                .build();

        assertNotSame(ipv4.hashCode(), ipv4i.hashCode());

        assertNotSame(ipv4.getAsNumber(), ipv4i.getAsNumber());

        assertEquals(ipv4.getNetworkAddress(), ipv4i.getNetworkAddress());
    }
}
