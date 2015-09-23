/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.openconfig;

import static org.opendaylight.protocol.bgp.openconfig.impl.util.OpenConfigUtil.BGP_IID;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigStateStore;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfiguration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.neighbors.NeighborKey;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

abstract class AbstractBGPNeighborProvider<T extends InstanceConfiguration> extends AbstractBGPOpenConfigMapper<T, Neighbor> {

    protected AbstractBGPNeighborProvider(final BindingTransactionChain txChain, final BGPConfigStateStore stateHolders,
            final Class<Neighbor> clazz) {
        super(txChain, stateHolders, clazz);
    }

    private static final InstanceIdentifier<Neighbors> NEIGHBORS_IID = BGP_IID.child(Neighbors.class);

    @Override
    protected InstanceIdentifier<Neighbor> getInstanceIdentifier(final Identifier key) {
        Preconditions.checkArgument((org.opendaylight.yangtools.yang.binding.Identifier<?>)key instanceof NeighborKey);
        return NEIGHBORS_IID.child(Neighbor.class, (NeighborKey) key);
    }

    @Override
    public final NeighborKey keyForConfiguration(final Neighbor neighbor) {
        return neighbor.getKey();
    }
}
