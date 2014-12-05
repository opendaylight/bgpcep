/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing;

import java.net.InetSocketAddress;
import org.opendaylight.protocol.pcep.ietf.initiated00.Stateful07SessionProposalFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev140506.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev140506.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev140506.sr.pce.capability.tlv.SrPceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public class SegmentRoutingSessionProposalFactory extends Stateful07SessionProposalFactory {

    private final boolean isSegmentRoutingCapable;

    public SegmentRoutingSessionProposalFactory(int deadTimer, int keepAlive, boolean stateful, boolean active,
            boolean instant, boolean isSegmentRoutingCapable) {
        super(deadTimer, keepAlive, stateful, active, instant);
        this.isSegmentRoutingCapable = isSegmentRoutingCapable;
    }

    @Override
    protected void addTlvs(InetSocketAddress address, TlvsBuilder builder) {
        super.addTlvs(address, builder);
        if (this.isSegmentRoutingCapable) {
            builder.addAugmentation(Tlvs1.class,
                    new Tlvs1Builder().setSrPceCapability(new SrPceCapabilityBuilder().setMsd((short) 0).build())
                            .build());
        }
    }

    public boolean isSegmentRoutingCapable() {
        return this.isSegmentRoutingCapable;
    }

}
