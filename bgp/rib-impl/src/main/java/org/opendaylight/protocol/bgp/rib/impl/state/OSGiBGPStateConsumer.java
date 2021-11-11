/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.state;

import java.util.List;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRibStateConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component(immediate = true, service = BGPStateConsumer.class)
public final class OSGiBGPStateConsumer extends AbstractBGPStateConsumer {
    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    volatile List<BGPRibStateConsumer> ribStates;
    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    volatile List<BGPPeerStateConsumer> peerStates;

    @Override
    List<BGPRibStateConsumer> bgpRibStates() {
        return ribStates;
    }

    @Override
    List<BGPPeerStateConsumer> bgpPeerStates() {
        return peerStates;
    }
}
