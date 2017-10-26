/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.initiated00;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07StatefulCapabilityTlvParser;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.stateful.capability.tlv.StatefulBuilder;

/**
 * Parser for {@link Stateful}
 */
public class CInitiated00StatefulCapabilityTlvParser extends Stateful07StatefulCapabilityTlvParser {

    protected static final int I_FLAG_OFFSET = 29;

    @Override
    protected void parseFlags(final StatefulBuilder sb, final ByteBuf buffer) {
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_F_LENGTH);
        sb.setLspUpdateCapability(flags.get(U_FLAG_OFFSET));
        if (flags.get(I_FLAG_OFFSET)) {
            sb.addAugmentation(Stateful1.class, new Stateful1Builder().setInitiation(Boolean.TRUE).build());
        }
    }

    @Override
    protected BitArray serializeFlags(final Stateful sct) {
        final BitArray flags = new BitArray(FLAGS_F_LENGTH);
        final Stateful1 sfi = sct.getAugmentation(Stateful1.class);
        if (sfi != null) {
            flags.set(I_FLAG_OFFSET, sfi.isInitiation());
        }
        flags.set(U_FLAG_OFFSET, sct.isLspUpdateCapability());
        return flags;
    }
}
