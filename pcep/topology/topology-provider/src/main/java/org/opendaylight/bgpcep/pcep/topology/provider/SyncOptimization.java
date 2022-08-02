/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.Tlvs;
import org.opendaylight.yangtools.concepts.Immutable;

final class SyncOptimization implements Immutable {
    private final boolean dbVersionMatch;
    private final boolean isSyncAvoidanceEnabled;
    private final boolean isDeltaSyncEnabled;
    private final boolean isDbVersionPresent;
    private final boolean isTriggeredInitialSyncEnable;
    private final boolean isTriggeredReSyncEnable;

    SyncOptimization(final Tlvs local, final Tlvs remote) {
        final var localLspDbVersion = getLspDbVersion(local);
        final var remoteLspDbVersion = getLspDbVersion(remote);
        dbVersionMatch = localLspDbVersion != null && localLspDbVersion.equals(remoteLspDbVersion);
        isDbVersionPresent = localLspDbVersion != null || remoteLspDbVersion != null;

        final var localStateful = getStateful1(local);
        final var removeStateful = getStateful1(remote);

        isSyncAvoidanceEnabled = isSyncAvoidance(localStateful) && isSyncAvoidance(removeStateful);
        isDeltaSyncEnabled = isDeltaSync(localStateful) && isDeltaSync(removeStateful);
        isTriggeredInitialSyncEnable = isTriggeredInitialSync(localStateful) && isTriggeredInitialSync(removeStateful)
                && (isDeltaSyncEnabled || isSyncAvoidanceEnabled);
        isTriggeredReSyncEnable = isTriggeredReSync(localStateful) && isTriggeredReSync(removeStateful);
    }

    boolean doesLspDbMatch() {
        return dbVersionMatch;
    }

    boolean isSyncAvoidanceEnabled() {
        return isSyncAvoidanceEnabled;
    }

    boolean isDeltaSyncEnabled() {
        return isDeltaSyncEnabled;
    }

    boolean isTriggeredInitSyncEnabled() {
        return isTriggeredInitialSyncEnable;
    }

    boolean isTriggeredReSyncEnabled() {
        return isTriggeredReSyncEnable;
    }

    boolean isDbVersionPresent() {
        return isDbVersionPresent;
    }

    private static LspDbVersion getLspDbVersion(final Tlvs openTlvs) {
        if (openTlvs != null) {
            final var tlvs3 = openTlvs.augmentation(Tlvs3.class);
            if (tlvs3 != null) {
                final var dbVersion = tlvs3.getLspDbVersion();
                if (dbVersion != null && dbVersion.getLspDbVersionValue() != null) {
                    return dbVersion;
                }
            }
        }
        return null;
    }

    private static Stateful1 getStateful1(final Tlvs openTlvs) {
        if (openTlvs != null) {
            final var tlvs1 = openTlvs.augmentation(Tlvs1.class);
            if (tlvs1 != null) {
                final var stateful = tlvs1.getStateful();
                if (stateful != null) {
                    return stateful.augmentation(Stateful1.class);
                }
            }
        }
        return null;
    }

    private static boolean isSyncAvoidance(final Stateful1 stateful) {
        return stateful != null && Boolean.TRUE.equals(stateful.getIncludeDbVersion());
    }

    private static boolean isDeltaSync(final Stateful1 stateful) {
        return stateful != null && Boolean.TRUE.equals(stateful.getDeltaLspSyncCapability());
    }

    private static boolean isTriggeredInitialSync(final Stateful1 stateful) {
        return stateful != null && Boolean.TRUE.equals(stateful.getTriggeredInitialSync());
    }

    private static boolean isTriggeredReSync(final Stateful1 stateful) {
        return stateful != null && Boolean.TRUE.equals(stateful.getTriggeredResync());
    }
}
