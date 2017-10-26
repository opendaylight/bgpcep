/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Optional;
import java.math.BigInteger;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.pcep.pcc.mock.api.PCCSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;

final class PCCSyncOptimization {
    private final boolean dbVersionMatch;
    private final boolean isSyncAvoidanceEnabled;
    private final boolean isDeltaSyncEnabled;
    private final boolean isTriggeredInitialSynEnable;
    private final boolean isTriggeredReSyncEnable;
    private final LspDbVersion localLspDbVersion;
    private final LspDbVersion remoteLspDbVersion;
    private BigInteger lspDBVersion = BigInteger.ONE;
    private Boolean resynchronizing = Boolean.FALSE;

    public PCCSyncOptimization(@Nonnull final PCCSession session) {
        requireNonNull(session);
        final Tlvs remote = session.getRemoteTlvs();
        final Tlvs local = session.localSessionCharacteristics();
        this.localLspDbVersion = getLspDbVersion(local);
        this.remoteLspDbVersion = getLspDbVersion(remote);
        this.dbVersionMatch = compareLspDbVersion(this.localLspDbVersion, this.remoteLspDbVersion);
        this.isSyncAvoidanceEnabled = isSyncAvoidance(local) && isSyncAvoidance(remote);
        this.isDeltaSyncEnabled = isDeltaSync(local) && isDeltaSync(remote);
        this.isTriggeredInitialSynEnable = isTriggeredInitialSync(local) && isTriggeredInitialSync(remote) &&
            (this.isDeltaSyncEnabled || this.isSyncAvoidanceEnabled);
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

    public BigInteger getLocalLspDbVersionValue() {
        if (this.localLspDbVersion == null) {
            return null;
        }
        return this.localLspDbVersion.getLspDbVersionValue();
    }

    public BigInteger getRemoteLspDbVersionValue() {
        if (this.remoteLspDbVersion == null) {
            return BigInteger.ONE;
        }
        return this.remoteLspDbVersion.getLspDbVersionValue();
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
        if (local != null && remote != null) {
            return local.equals(remote);
        }
        return false;
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

    public Optional<BigInteger> incrementLspDBVersion() {
        if (!isSyncAvoidanceEnabled()) {
            return Optional.absent();
        } else if (isSyncNeedIt() && getLocalLspDbVersionValue() != null && !this.resynchronizing) {
            this.lspDBVersion = getLocalLspDbVersionValue();
            return Optional.of(this.lspDBVersion);
        } else if (this.resynchronizing) {
            return Optional.of(this.lspDBVersion);
        }

        this.lspDBVersion = this.lspDBVersion.add(BigInteger.ONE);
        return Optional.of(this.lspDBVersion);
    }

    public boolean isSyncNeedIt() {
        if (doesLspDbMatch() && !this.resynchronizing) {
            return false;
        }
        return true;
    }

    public void setResynchronizingState(final Boolean resync) {
        this.resynchronizing = resync;
    }
}
