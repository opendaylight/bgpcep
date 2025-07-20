/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.p2mp.te.lsp;

import java.net.InetSocketAddress;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.p2mp.te.lsp.rev181109.TlvsP2mpCapabilityAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.p2mp.te.lsp.rev181109.p2mp.pce.capability.tlv.P2mpPceCapabilityBuilder;

public final class P2MPTeLspCapability extends PCEPCapability {
    private static final @NonNull P2MPTeLspCapability INSTANCE = new P2MPTeLspCapability();

    private P2MPTeLspCapability() {
        // Hidden on purpose
    }

    public static @NonNull P2MPTeLspCapability of() {
        return INSTANCE;
    }

    @Override
    public void setCapabilityProposal(final InetSocketAddress address, final TlvsBuilder builder) {
        builder.addAugmentation(new TlvsP2mpCapabilityAugBuilder()
            .setP2mpPceCapability(new P2mpPceCapabilityBuilder().build())
            .build());
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj;
    }
}
