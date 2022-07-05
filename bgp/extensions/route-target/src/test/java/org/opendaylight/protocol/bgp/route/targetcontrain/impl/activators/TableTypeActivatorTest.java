/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.route.targetcontrain.impl.activators;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.ROUTETARGETCONSTRAIN;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.route.target.constrain.rev180618.RouteTargetConstrainSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;

public final class TableTypeActivatorTest {
    private static final BgpTableType RT_TP = new BgpTableTypeImpl(
            Ipv4AddressFamily.VALUE, RouteTargetConstrainSubsequentAddressFamily.VALUE);

    @Test
    public void testActivator() {
        var registry = BGPTableTypeRegistryConsumer.of(new TableTypeActivator());
        assertEquals(ROUTETARGETCONSTRAIN.class, registry.getAfiSafiType(RT_TP));
        assertEquals(RT_TP, registry.getTableType(ROUTETARGETCONSTRAIN.class));
    }
}