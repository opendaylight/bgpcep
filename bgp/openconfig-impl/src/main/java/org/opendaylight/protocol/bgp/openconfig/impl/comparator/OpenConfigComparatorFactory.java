/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.comparator;

import com.google.common.collect.ImmutableMap;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.OpenConfigComparator;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class OpenConfigComparatorFactory {

    private static final ImmutableMap<Class<?>, OpenConfigComparator<? extends DataObject>> COMPARATORS = ImmutableMap.<Class<?>, OpenConfigComparator<? extends DataObject>>builder()
            .put(Global.class, new GlobalComparator())
            .put(Neighbor.class, new NeighborComparator())
            .build();

    public static <T extends DataObject> OpenConfigComparator<T> getComparator(final Class<T> clazz) {
        return (OpenConfigComparator<T>) COMPARATORS.get(clazz);
    }

}
