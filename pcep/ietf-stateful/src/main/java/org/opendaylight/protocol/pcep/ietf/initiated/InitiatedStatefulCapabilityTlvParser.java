/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.initiated;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.ietf.stateful.StatefulStatefulCapabilityTlvParser;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.stateful.capability.tlv.StatefulBuilder;

/**
 * Parser for {@link Stateful}.
 */
public class InitiatedStatefulCapabilityTlvParser extends StatefulStatefulCapabilityTlvParser {

    protected static final int I_FLAG_OFFSET = 29;

    @Override
    protected void parseFlags(final StatefulBuilder sb, final ByteBuf buffer) {
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_F_LENGTH);
        sb.setLspUpdateCapability(flags.get(U_FLAG_OFFSET));
        if (flags.get(I_FLAG_OFFSET)) {
            sb.addAugmentation(new Stateful1Builder().setInitiation(Boolean.TRUE).build());
        }
    }

    @Override
    protected BitArray serializeFlags(final Stateful sct) {
        final BitArray flags = new BitArray(FLAGS_F_LENGTH);
        final Stateful1 sfi = sct.augmentation(Stateful1.class);
        if (sfi != null) {
            flags.set(I_FLAG_OFFSET, sfi.getInitiation());
        }
        flags.set(U_FLAG_OFFSET, sct.getLspUpdateCapability());
        return flags;
    }
}
