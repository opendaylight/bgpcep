/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.p2mp.te.lsp;

import com.google.common.base.MoreObjects;
import java.net.InetSocketAddress;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.p2mp.te.lsp.rev181109.TlvsP2mpCapabilityAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.p2mp.te.lsp.rev181109.p2mp.pce.capability.tlv.P2mpPceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;

@MetaInfServices
public final class P2MPTeLspCapability implements PCEPCapability {
    @Override
    public void setCapabilityProposal(final InetSocketAddress address, final TlvsBuilder builder) {
        builder.addAugmentation(new TlvsP2mpCapabilityAugBuilder()
            .setP2mpPceCapability(new P2mpPceCapabilityBuilder().build())
            .build());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).toString();
    }
}
