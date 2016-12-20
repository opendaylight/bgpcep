/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.state;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRIBState;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateCollector;

public class BGPStateCollectorImpl implements BGPStateCollector {
    private final List<BGPRIBState> ribsStates = new ArrayList<>();

    @Override
    public List<BGPRIBState> getRibStats() {
        return ImmutableList.copyOf(this.ribsStates);
    }

    @Override
    public void bind(final BGPRIBState service) {
        this.ribsStates.add(service);
    }

    @Override
    public void unbind(final BGPRIBState reference) {
        this.ribsStates.remove(reference);
    }
}
