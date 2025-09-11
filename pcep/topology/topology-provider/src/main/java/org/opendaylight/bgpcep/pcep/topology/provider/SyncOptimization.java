/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.stateful.capability.tlv.StatefulCapability;

record SyncOptimization(
        boolean dbVersionMatch,
        boolean syncAvoidanceEnabled,
        boolean deltaSyncEnabled,
        boolean dbVersionPresent,
        boolean triggeredInitialSyncEnabled,
        boolean triggeredReSyncEnabled) {
    static @NonNull SyncOptimization of(final Tlvs local, final Tlvs remote) {
        final var localLspDbVersion = lspDbVersion(local);
        final var remoteLspDbVersion = lspDbVersion(remote);
        final var localStateful = stateful(local);
        final var removeStateful = stateful(remote);

        final var syncAvoidanceEnabled = syncAvoidance(localStateful) && syncAvoidance(removeStateful);
        final var deltaSyncEnabled = deltaSync(localStateful) && deltaSync(removeStateful);

        return new SyncOptimization(
            localLspDbVersion != null && localLspDbVersion.equals(remoteLspDbVersion),
            syncAvoidanceEnabled, deltaSyncEnabled,
            localLspDbVersion != null || remoteLspDbVersion != null,
            (deltaSyncEnabled || syncAvoidanceEnabled)
                && triggeredInitialSync(localStateful) && triggeredInitialSync(removeStateful),
            triggeredReSync(localStateful) && triggeredReSync(removeStateful));
    }

    private static @Nullable LspDbVersion lspDbVersion(final Tlvs openTlvs) {
        if (openTlvs != null) {
            final var dbVersion = openTlvs.getLspDbVersion();
            if (dbVersion != null && dbVersion.getLspDbVersionValue() != null) {
                return dbVersion;
            }
        }
        return null;
    }

    private static @Nullable StatefulCapability stateful(final Tlvs openTlvs) {
        if (openTlvs != null) {
            return openTlvs.getStatefulCapability();
        }
        return null;
    }

    private static boolean syncAvoidance(final StatefulCapability stateful) {
        return stateful != null && Boolean.TRUE.equals(stateful.getIncludeDbVersion());
    }

    private static boolean deltaSync(final StatefulCapability stateful) {
        return stateful != null && Boolean.TRUE.equals(stateful.getDeltaLspSyncCapability());
    }

    private static boolean triggeredInitialSync(final StatefulCapability stateful) {
        return stateful != null && Boolean.TRUE.equals(stateful.getTriggeredInitialSync());
    }

    private static boolean triggeredReSync(final StatefulCapability stateful) {
        return stateful != null && Boolean.TRUE.equals(stateful.getTriggeredResync());
    }
}
