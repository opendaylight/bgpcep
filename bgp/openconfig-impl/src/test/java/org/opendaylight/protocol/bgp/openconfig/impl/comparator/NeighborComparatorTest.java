/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.comparator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6UNICAST;

public class NeighborComparatorTest {

    private static final NeighborComparator COMPARATOR = new NeighborComparator();

    @Test
    public void testIsSamePositiveSimple() {
        final Neighbor a = new NeighborBuilder().build();
        final Neighbor b = new NeighborBuilder().build();
        assertTrue(COMPARATOR.isSame(a, b));
    }

    @Test
    public void testIsSamePositiveCollection() {
        final Neighbor a = new NeighborBuilder().setAfiSafis(new AfiSafisBuilder()
            .setAfiSafi(Lists.newArrayList(
                    new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class).build(),
                    new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class).build()))
            .build()).build();
        final Neighbor b = new NeighborBuilder().setAfiSafis(new AfiSafisBuilder()
            .setAfiSafi(Lists.newArrayList(
                new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class).build(),
                new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class).build()))
            .build()).build();
        assertTrue(COMPARATOR.isSame(a, b));
    }

    @Test
    public void testIsSameNegativeCollectionSize() {
        final Neighbor a = new NeighborBuilder().setAfiSafis(new AfiSafisBuilder()
            .setAfiSafi(Lists.newArrayList(
                    new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class).build(),
                    new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class).build()))
            .build()).build();
        final Neighbor b = new NeighborBuilder().setAfiSafis(new AfiSafisBuilder()
            .setAfiSafi(Lists.newArrayList(
                new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class).build()))
            .build()).build();
        assertFalse(COMPARATOR.isSame(a, b));
    }

    @Test
    public void testIsSameNegativeCollectionContent() {
        final Neighbor a = new NeighborBuilder().setAfiSafis(new AfiSafisBuilder()
            .setAfiSafi(Lists.newArrayList(
                    new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class).build()))
            .build()).build();
        final Neighbor b = new NeighborBuilder().setAfiSafis(new AfiSafisBuilder()
            .setAfiSafi(Lists.newArrayList(
                new AfiSafiBuilder().setAfiSafiName(IPV6UNICAST.class).build()))
            .build()).build();
        assertFalse(COMPARATOR.isSame(a, b));
    }

}
