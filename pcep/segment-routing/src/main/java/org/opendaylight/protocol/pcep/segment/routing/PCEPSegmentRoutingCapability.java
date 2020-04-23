/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.segment.routing;

import java.net.InetSocketAddress;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev181109.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev181109.sr.pce.capability.tlv.SrPceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;
import org.opendaylight.yangtools.yang.common.Uint8;

public class PCEPSegmentRoutingCapability implements PCEPCapability {

    private final boolean isSegmentRoutingCapable;

    public PCEPSegmentRoutingCapability(final boolean isSegmentRoutingCapable) {
        this.isSegmentRoutingCapable = isSegmentRoutingCapable;
    }

    @Override
    public void setCapabilityProposal(final InetSocketAddress address, final TlvsBuilder builder) {
        if (this.isSegmentRoutingCapable) {
            builder.addAugmentation(new Tlvs1Builder()
                .setSrPceCapability(new SrPceCapabilityBuilder().setMsd(Uint8.ZERO).build())
                .build());
        }
    }

    public boolean isSegmentRoutingCapable() {
        return this.isSegmentRoutingCapable;
    }

    @Override
    public boolean isStateful() {
        return false;
    }
}
