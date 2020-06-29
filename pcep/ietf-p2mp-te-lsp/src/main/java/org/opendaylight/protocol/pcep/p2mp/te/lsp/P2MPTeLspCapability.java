/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.p2mp.te.lsp;

import java.net.InetSocketAddress;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.p2mp.te.lsp.rev181109.TlvsP2mpCapabilityAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.p2mp.te.lsp.rev181109.TlvsP2mpCapabilityAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.p2mp.te.lsp.rev181109.p2mp.pce.capability.tlv.P2mpPceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;

public final class P2MPTeLspCapability implements PCEPCapability {
    private static final TlvsP2mpCapabilityAug PATH_COMPUTATION_CAP_AUG = new TlvsP2mpCapabilityAugBuilder()
        .setP2mpPceCapability(new P2mpPceCapabilityBuilder().build()).build();
    private final boolean supportsPathComputation;

    public P2MPTeLspCapability(final boolean supportsP2MPTeLspPathComputation) {
        this.supportsPathComputation = supportsP2MPTeLspPathComputation;
    }

    @Override
    public void setCapabilityProposal(final InetSocketAddress address, final TlvsBuilder builder) {
        if (this.supportsPathComputation) {
            builder.addAugmentation(PATH_COMPUTATION_CAP_AUG);
        }
    }

    @Override
    public boolean isStateful() {
        return false;
    }
}
