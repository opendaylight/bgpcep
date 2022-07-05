/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled.unicast;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4LABELLEDUNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6LABELLEDUNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.LabeledUnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;

public class TableTypeActivatorTest {
    private static final BgpTableType IPV4 = new BgpTableTypeImpl(Ipv4AddressFamily.VALUE,
        LabeledUnicastSubsequentAddressFamily.VALUE);
    private static final BgpTableType IPV6 = new BgpTableTypeImpl(Ipv6AddressFamily.VALUE,
        LabeledUnicastSubsequentAddressFamily.VALUE);

    @Test
    public void testActivator() {
        var registry = BGPTableTypeRegistryConsumer.of(new TableTypeActivator());
        assertEquals(IPV4LABELLEDUNICAST.VALUE, registry.getAfiSafiType(IPV4));
        assertEquals(IPV6LABELLEDUNICAST.VALUE, registry.getAfiSafiType(IPV6));

        assertEquals(IPV4, registry.getTableType(IPV4LABELLEDUNICAST.VALUE));
        assertEquals(IPV6, registry.getTableType(IPV6LABELLEDUNICAST.VALUE));
    }
}
