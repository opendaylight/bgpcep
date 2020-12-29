/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.sync.optimizations;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.ietf.initiated.InitiatedStatefulCapabilityTlvParser;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.stateful.capability.tlv.StatefulBuilder;

public class SyncOptimizationsCapabilityTlvParser extends InitiatedStatefulCapabilityTlvParser {

    protected static final int S_FLAG_OFFSET = 30;
    protected static final int T_FLAG_OFFSET = 28;
    protected static final int D_FLAG_OFFSET = 27;
    protected static final int F_FLAG_OFFSET = 26;

    @Override
    protected void parseFlags(final StatefulBuilder sb, final ByteBuf buffer) {
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_F_LENGTH);
        sb.setLspUpdateCapability(flags.get(U_FLAG_OFFSET));
        if (flags.get(I_FLAG_OFFSET)) {
            sb.addAugmentation(new Stateful1Builder().setInitiation(Boolean.TRUE).build());
        }
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations
            .rev200720.Stateful1Builder syncOptBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                .yang.controller.pcep.sync.optimizations.rev200720.Stateful1Builder();
        if (flags.get(S_FLAG_OFFSET)) {
            syncOptBuilder.setIncludeDbVersion(Boolean.TRUE);
        }
        if (flags.get(T_FLAG_OFFSET)) {
            syncOptBuilder.setTriggeredResync(Boolean.TRUE);
        }
        if (flags.get(D_FLAG_OFFSET)) {
            syncOptBuilder.setDeltaLspSyncCapability(Boolean.TRUE);
        }
        if (flags.get(F_FLAG_OFFSET)) {
            syncOptBuilder.setTriggeredInitialSync(Boolean.TRUE);
        }
        sb.addAugmentation(syncOptBuilder.build());
    }

    @Override
    protected BitArray serializeFlags(final Stateful sct) {
        final BitArray flags = new BitArray(FLAGS_F_LENGTH);
        final Stateful1 sfi = sct.augmentation(Stateful1.class);
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations
            .rev200720.Stateful1 sf2 = sct.augmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                .controller.pcep.sync.optimizations.rev200720.Stateful1.class);
        if (sf2 != null) {
            flags.set(F_FLAG_OFFSET, sf2.getTriggeredInitialSync());
            flags.set(D_FLAG_OFFSET, sf2.getDeltaLspSyncCapability());
            flags.set(T_FLAG_OFFSET, sf2.getTriggeredResync());
            flags.set(S_FLAG_OFFSET, sf2.getIncludeDbVersion());
        }
        if (sfi != null) {
            flags.set(I_FLAG_OFFSET, sfi.getInitiation());
        }
        flags.set(U_FLAG_OFFSET, sct.getLspUpdateCapability());
        return flags;
    }
}
