/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful;

import com.google.common.base.MoreObjects.ToStringHelper;
import java.net.InetSocketAddress;
import java.util.Arrays;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.config.rev230115.StatefulCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;

public final class PCEPStatefulCapability extends PCEPCapability {
    private final boolean active;
    private final boolean initiated;
    private final boolean triggeredSync;
    private final boolean triggeredResync;
    private final boolean deltaLspSync;
    private final boolean includeDbVersion;

    public PCEPStatefulCapability() {
        this(true, true, true, true, true, true);
    }

    public PCEPStatefulCapability(final boolean active, final boolean initiated, final boolean triggeredSync,
            final boolean triggeredResync, final boolean deltaLspSync, final boolean includeDbVersion) {
        this.active = active;
        this.initiated = initiated;
        this.triggeredSync = triggeredSync;
        this.triggeredResync = triggeredResync;
        this.deltaLspSync = deltaLspSync;
        this.includeDbVersion = includeDbVersion || triggeredSync || deltaLspSync;
    }

    public PCEPStatefulCapability(final StatefulCapabilities config) {
        this(config.getActive(), config.getInitiated(), config.getTriggeredInitialSync(),
                config.getTriggeredResync(), config.getDeltaLspSyncCapability(), config.getIncludeDbVersion());
    }

    @Override
    public void setCapabilityProposal(final InetSocketAddress address, final TlvsBuilder builder) {
        builder.addAugmentation(new Tlvs1Builder()
            .setStateful(new StatefulBuilder().setLspUpdateCapability(active)
                .addAugmentation(new Stateful1Builder().setInitiation(initiated).build())
                .addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller
                    .pcep.sync.optimizations.rev200720.Stateful1Builder()
                        .setTriggeredInitialSync(triggeredSync)
                        .setTriggeredResync(triggeredResync)
                        .setDeltaLspSyncCapability(deltaLspSync)
                        .setIncludeDbVersion(includeDbVersion)
                        .build())
                .build())
            .build());
    }

    public boolean isActive() {
        return active;
    }

    public boolean isInstant() {
        return initiated;
    }

    public boolean isTriggeredSync() {
        return triggeredSync;
    }

    public boolean isTriggeredResync() {
        return triggeredResync;
    }

    public boolean isDeltaLspSync() {
        return deltaLspSync;
    }

    public boolean isIncludeDbVersion() {
        return includeDbVersion;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(
            new boolean[] { active, initiated, triggeredSync, triggeredResync, deltaLspSync, includeDbVersion });
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof PCEPStatefulCapability other && active == other.active
            && initiated == other.initiated && triggeredSync == other.triggeredSync
            && triggeredResync == other.triggeredResync && deltaLspSync == other.deltaLspSync
            && includeDbVersion == other.includeDbVersion;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        if (active) {
            helper.addValue("active");
        }
        if (initiated) {
            helper.addValue("initiated");
        }
        if (triggeredSync) {
            helper.addValue("triggeredSync");
        }
        if (triggeredResync) {
            helper.addValue("triggeredResync");
        }
        if (deltaLspSync) {
            helper.addValue("deltaLspSync");
        }
        if (includeDbVersion) {
            helper.addValue("includeDbVersion");
        }
        return helper;
    }
}
