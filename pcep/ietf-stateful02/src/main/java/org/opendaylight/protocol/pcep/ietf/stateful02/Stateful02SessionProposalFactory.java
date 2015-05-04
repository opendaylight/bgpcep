/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful02;

import java.net.InetSocketAddress;
import org.opendaylight.protocol.pcep.impl.BasePCEPSessionProposalFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.lsp.cleanup.tlv.LspCleanupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Tlvs2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Tlvs2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

@Deprecated
public class Stateful02SessionProposalFactory extends BasePCEPSessionProposalFactory {

    private final boolean stateful, active, instant;

    private final long timeout;

    public Stateful02SessionProposalFactory(final int deadTimer, final int keepAlive, final boolean stateful, final boolean active,
            final boolean instant, final int timeout) {
        super(deadTimer, keepAlive);
        this.stateful = stateful;
        this.active = active;
        this.instant = instant;
        this.timeout = timeout;
    }

    @Override
    protected void addTlvs(final InetSocketAddress address, final TlvsBuilder builder) {
        if (Stateful02SessionProposalFactory.this.stateful) {
            builder.addAugmentation(
                    Tlvs2.class,
                    new Tlvs2Builder().setStateful(
                            new StatefulBuilder().setLspUpdateCapability(this.active).addAugmentation(Stateful1.class,
                                    new Stateful1Builder().setInitiation(this.instant).build()).build()).build()).build();
            builder.addAugmentation(Tlvs1.class,
                    new Tlvs1Builder().setLspCleanup(new LspCleanupBuilder().setTimeout(this.timeout).build()).build());
        }
    }

    public boolean isStateful() {
        return this.stateful;
    }

    public boolean isActive() {
        return this.active;
    }

    public boolean isInstant() {
        return this.instant;
    }
}
