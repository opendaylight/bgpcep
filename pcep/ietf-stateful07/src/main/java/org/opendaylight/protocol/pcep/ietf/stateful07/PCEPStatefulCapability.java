/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import java.net.InetSocketAddress;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.ietf.stateful07.app.config.rev160707.PcepIetfStateful07Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public class PCEPStatefulCapability implements PCEPCapability {

    private final boolean stateful, active, instant, triggeredSync, triggeredResync, deltaLspSync, includeDbVersion;

    public PCEPStatefulCapability(final boolean stateful, final boolean active, final boolean instant,
        final boolean triggeredSync, final boolean triggeredResync, final boolean deltaLspSync, final boolean includeDbVersion){
        this.stateful = stateful || active || triggeredSync || triggeredResync || deltaLspSync || includeDbVersion;
        this.active = active;
        this.instant = instant;
        this.triggeredSync = triggeredSync;
        this.triggeredResync = triggeredResync;
        this.deltaLspSync = deltaLspSync;
        this.includeDbVersion = includeDbVersion || triggeredSync || deltaLspSync;
    }

    public PCEPStatefulCapability(PcepIetfStateful07Config config) {
        this(config.isStateful(), config.isActive(), config.isInitiated(), config.isTriggeredInitialSync(),
                config.isTriggeredResync(), config.isDeltaLspSyncCapability(), config.isIncludeDbVersion());
    }

    @Override
    public void setCapabilityProposal(final InetSocketAddress address, final TlvsBuilder builder) {
        if (this.stateful) {
            builder.addAugmentation(
                Tlvs1.class, new Tlvs1Builder()
                    .setStateful( new StatefulBuilder().setLspUpdateCapability(this.active)
                        .addAugmentation(Stateful1.class, new Stateful1Builder().setInitiation(this.instant).build())
                        .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Stateful1.class, new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Stateful1Builder()
                            .setTriggeredInitialSync(this.triggeredSync)
                            .setTriggeredResync(this.triggeredResync)
                            .setDeltaLspSyncCapability(this.deltaLspSync)
                            .setIncludeDbVersion(this.includeDbVersion)
                            .build())
                        .build())
                    .build());
        }
    }

    @Override
    public boolean isStateful() {
        return this.stateful;
    }

    public boolean isActive() {
        return this.active;
    }

    public boolean isInstant() {
        return this.instant;
    }

    public boolean isTriggeredSync() {
        return this.triggeredSync;
    }

    public boolean isTriggeredResync() {
        return this.triggeredResync;
    }

    public boolean isDeltaLspSync() {
        return this.deltaLspSync;
    }

    public boolean isIncludeDbVersion() {
        return this.includeDbVersion;
    }
}
