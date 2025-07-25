/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.open.object.open.Tlvs;
import org.opendaylight.yangtools.yang.common.Uint64;

final class PCCSyncOptimization {
    private final boolean dbVersionMatch;
    private final boolean isSyncAvoidanceEnabled;
    private final boolean isDeltaSyncEnabled;
    private final boolean isTriggeredInitialSynEnable;
    private final boolean isTriggeredReSyncEnable;
    private final LspDbVersion localLspDbVersion;
    private final LspDbVersion remoteLspDbVersion;

    private Uint64 lspDBVersion = Uint64.ONE;
    private boolean resynchronizing = false;

    PCCSyncOptimization(final @NonNull PCCSession session) {
        requireNonNull(session);
        final Tlvs remote = session.getRemoteTlvs();
        final Tlvs local = session.localSessionCharacteristics();
        this.localLspDbVersion = getLspDbVersion(local);
        this.remoteLspDbVersion = getLspDbVersion(remote);
        this.dbVersionMatch = compareLspDbVersion(this.localLspDbVersion, this.remoteLspDbVersion);
        this.isSyncAvoidanceEnabled = isSyncAvoidance(local) && isSyncAvoidance(remote);
        this.isDeltaSyncEnabled = isDeltaSync(local) && isDeltaSync(remote);
        this.isTriggeredInitialSynEnable = isTriggeredInitialSync(local) && isTriggeredInitialSync(remote)
                && (this.isDeltaSyncEnabled || this.isSyncAvoidanceEnabled);
        this.isTriggeredReSyncEnable = isTriggeredReSync(local) && isTriggeredReSync(remote);
    }

    public boolean doesLspDbMatch() {
        return this.dbVersionMatch;
    }

    public boolean isSyncAvoidanceEnabled() {
        return this.isSyncAvoidanceEnabled;
    }

    public boolean isDeltaSyncEnabled() {
        return this.isDeltaSyncEnabled;
    }

    public boolean isTriggeredInitSyncEnabled() {
        return this.isTriggeredInitialSynEnable;
    }

    public boolean isTriggeredReSyncEnabled() {
        return this.isTriggeredReSyncEnable;
    }

    public Uint64 getLocalLspDbVersionValue() {
        if (this.localLspDbVersion == null) {
            return null;
        }
        return this.localLspDbVersion.getLspDbVersionValue();
    }

    public Uint64 getRemoteLspDbVersionValue() {
        if (this.remoteLspDbVersion == null) {
            return Uint64.ONE;
        }
        return this.remoteLspDbVersion.getLspDbVersionValue();
    }

    private static LspDbVersion getLspDbVersion(final Tlvs openTlvs) {
        if (openTlvs != null) {
            final Tlvs3 tlvs3 = openTlvs.augmentation(Tlvs3.class);
            if (tlvs3 != null && tlvs3.getLspDbVersion() != null
                && tlvs3.getLspDbVersion().getLspDbVersionValue() != null) {
                return tlvs3.getLspDbVersion();
            }
        }
        return null;
    }

    private static boolean compareLspDbVersion(final LspDbVersion local, final LspDbVersion remote) {
        if (local != null && remote != null) {
            return local.equals(remote);
        }
        return false;
    }

    private static Stateful1 getStateful1(final Tlvs openTlvs) {
        if (openTlvs != null) {
            final Tlvs1 tlvs1 = openTlvs.augmentation(Tlvs1.class);
            if (tlvs1 != null && tlvs1.getStateful() != null) {
                return tlvs1.getStateful().augmentation(Stateful1.class);
            }
        }
        return null;
    }

    private static boolean isSyncAvoidance(final Tlvs openTlvs) {
        final Stateful1 stateful1 = getStateful1(openTlvs);
        return stateful1 != null && Boolean.TRUE.equals(stateful1.getIncludeDbVersion());
    }

    private static boolean isDeltaSync(final Tlvs openTlvs) {
        final Stateful1 stateful1 = getStateful1(openTlvs);
        return stateful1 != null && Boolean.TRUE.equals(stateful1.getDeltaLspSyncCapability());
    }

    private static boolean isTriggeredInitialSync(final Tlvs openTlvs) {
        final Stateful1 stateful1 = getStateful1(openTlvs);
        return stateful1 != null && Boolean.TRUE.equals(stateful1.getTriggeredInitialSync());
    }

    private static boolean isTriggeredReSync(final Tlvs openTlvs) {
        final Stateful1 stateful1 = getStateful1(openTlvs);
        return stateful1 != null && Boolean.TRUE.equals(stateful1.getTriggeredResync());
    }

    public Optional<Uint64> incrementLspDBVersion() {
        if (!isSyncAvoidanceEnabled()) {
            return Optional.empty();
        } else if (isSyncNeedIt() && getLocalLspDbVersionValue() != null && !this.resynchronizing) {
            this.lspDBVersion = getLocalLspDbVersionValue();
            return Optional.of(this.lspDBVersion);
        } else if (this.resynchronizing) {
            return Optional.of(this.lspDBVersion);
        }

        this.lspDBVersion = Uint64.fromLongBits(lspDBVersion.longValue() + 1);
        return Optional.of(this.lspDBVersion);
    }

    public boolean isSyncNeedIt() {
        return !doesLspDbMatch() || this.resynchronizing;
    }

    void setResynchronizingState(final boolean resync) {
        this.resynchronizing = resync;
    }
}
