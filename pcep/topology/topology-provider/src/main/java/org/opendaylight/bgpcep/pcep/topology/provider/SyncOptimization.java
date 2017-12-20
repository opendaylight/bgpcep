/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.topology.provider;

import static java.util.Objects.requireNonNull;

import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;

final class SyncOptimization {

    private final boolean dbVersionMatch;
    private final boolean isSyncAvoidanceEnabled;
    private final boolean isDeltaSyncEnabled;
    private final boolean isDbVersionPresent;
    private final boolean isTriggeredInitialSyncEnable;
    private final boolean isTriggeredReSyncEnable;

    SyncOptimization(final PCEPSession session) {
        requireNonNull(session);
        final Tlvs remote = session.getRemoteTlvs();
        final Tlvs local = session.getLocalTlvs();
        final LspDbVersion localLspDbVersion = getLspDbVersion(local);
        final LspDbVersion remoteLspDbVersion = getLspDbVersion(remote);
        this.dbVersionMatch = compareLspDbVersion(localLspDbVersion, remoteLspDbVersion);
        this.isSyncAvoidanceEnabled = isSyncAvoidance(local) && isSyncAvoidance(remote);
        this.isDeltaSyncEnabled = isDeltaSync(local) && isDeltaSync(remote);
        this.isDbVersionPresent = localLspDbVersion != null || remoteLspDbVersion != null;
        this.isTriggeredInitialSyncEnable = isTriggeredInitialSync(local) && isTriggeredInitialSync(remote)
                && (this.isDeltaSyncEnabled || this.isSyncAvoidanceEnabled);
        this.isTriggeredReSyncEnable = isTriggeredReSync(local) && isTriggeredReSync(remote);
    }

    boolean doesLspDbMatch() {
        return this.dbVersionMatch;
    }

    boolean isSyncAvoidanceEnabled() {
        return this.isSyncAvoidanceEnabled;
    }

    boolean isDeltaSyncEnabled() {
        return this.isDeltaSyncEnabled;
    }

    boolean isTriggeredInitSyncEnabled() {
        return this.isTriggeredInitialSyncEnable;
    }

    boolean isTriggeredReSyncEnabled() {
        return this.isTriggeredReSyncEnable;
    }

    boolean isDbVersionPresent() {
        return this.isDbVersionPresent;
    }

    private static LspDbVersion getLspDbVersion(final Tlvs openTlvs) {
        if (openTlvs != null) {
            final Tlvs3 tlvs3 = openTlvs.getAugmentation(Tlvs3.class);
            if (tlvs3 != null && tlvs3.getLspDbVersion() != null
                    && tlvs3.getLspDbVersion().getLspDbVersionValue() != null) {
                return tlvs3.getLspDbVersion();
            }
        }
        return null;
    }

    private static boolean compareLspDbVersion(final LspDbVersion local, final LspDbVersion remote) {
        return local != null && remote != null && local.equals(remote);
    }

    private static Stateful1 getStateful1(final Tlvs openTlvs) {
        if (openTlvs != null) {
            final Tlvs1 tlvs1 = openTlvs.getAugmentation(Tlvs1.class);
            if (tlvs1 != null && tlvs1.getStateful() != null) {
                return tlvs1.getStateful().getAugmentation(Stateful1.class);
            }
        }
        return null;
    }

    private static boolean isSyncAvoidance(final Tlvs openTlvs) {
        final Stateful1 stateful1 = getStateful1(openTlvs);
        if (stateful1 != null && stateful1.isIncludeDbVersion() != null) {
            return stateful1.isIncludeDbVersion();
        }
        return false;
    }

    private static boolean isDeltaSync(final Tlvs openTlvs) {
        final Stateful1 stateful1 = getStateful1(openTlvs);
        if (stateful1 != null && stateful1.isDeltaLspSyncCapability() != null) {
            return stateful1.isDeltaLspSyncCapability();
        }
        return false;
    }

    private static boolean isTriggeredInitialSync(final Tlvs openTlvs) {
        final Stateful1 stateful1 = getStateful1(openTlvs);
        if (stateful1 != null && stateful1.isTriggeredInitialSync() != null) {
            return stateful1.isTriggeredInitialSync();
        }
        return false;
    }

    private static boolean isTriggeredReSync(final Tlvs openTlvs) {
        final Stateful1 stateful1 = getStateful1(openTlvs);
        if (stateful1 != null && stateful1.isTriggeredResync() != null) {
            return stateful1.isTriggeredResync();
        }
        return false;
    }
}
