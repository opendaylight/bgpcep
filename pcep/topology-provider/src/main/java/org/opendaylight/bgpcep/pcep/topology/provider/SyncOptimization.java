/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.base.Preconditions;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;

final class SyncOptimization {

    private final boolean dbVersionMatch;
    private final boolean isSyncAvoidanceEnabled;
    private final boolean isDbVersionPresent;

    public SyncOptimization(final PCEPSession session) {
        Preconditions.checkNotNull(session);
        final Tlvs remote = session.getRemoteTlvs();
        final Tlvs local = session.localSessionCharacteristics();
        final LspDbVersion localLspDbVersion = getLspDbVersion(local);
        final LspDbVersion remoteLspDbVersion = getLspDbVersion(remote);
        this.dbVersionMatch = compareLspDbVersion(localLspDbVersion, remoteLspDbVersion);
        this.isSyncAvoidanceEnabled = isSyncAvoidance(local) && isSyncAvoidance(remote);
        this.isDbVersionPresent = localLspDbVersion != null || remoteLspDbVersion != null;
    }

    public boolean doesLspDbMatch() {
        return dbVersionMatch;
    }

    public boolean isSyncAvoidanceEnabled() {
        return isSyncAvoidanceEnabled;
    }

    public boolean isDbVersionPresent() {
        return isDbVersionPresent;
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

    private static boolean isSyncAvoidance(final Tlvs openTlvs) {
        if (openTlvs != null) {
            final Tlvs1 tlvs1 = openTlvs.getAugmentation(Tlvs1.class);
            if (tlvs1 != null && tlvs1.getStateful() != null) {
                final Stateful1 stateful1 = tlvs1.getStateful().getAugmentation(Stateful1.class);
                if (stateful1 != null && stateful1.isIncludeDbVersion() != null) {
                    return stateful1.isIncludeDbVersion();
                }
            }
        }
        return false;
    }

}
