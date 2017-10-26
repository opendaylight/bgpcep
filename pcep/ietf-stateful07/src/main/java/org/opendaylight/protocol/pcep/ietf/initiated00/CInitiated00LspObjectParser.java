/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.initiated00;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07LspObjectParser;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Lsp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Lsp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.LspBuilder;

/**
 * Parser for {@link Lsp}
 */
public class CInitiated00LspObjectParser extends Stateful07LspObjectParser {

    private static final int CREATE_FLAG_OFFSET = 4;

    public CInitiated00LspObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg);
    }

    @Override
    protected void parseFlags(final LspBuilder builder, final ByteBuf bytes) {
        final BitArray flags = BitArray.valueOf(bytes, FLAGS_SIZE);
        builder.setDelegate(flags.get(DELEGATE));
        builder.setSync(flags.get(SYNC));
        builder.setRemove(flags.get(REMOVE));
        builder.setAdministrative(flags.get(ADMINISTRATIVE));
        builder.addAugmentation(Lsp1.class, new Lsp1Builder().setCreate(flags.get(CREATE_FLAG_OFFSET)).build());
        short s = 0;
        s |= flags.get(OPERATIONAL + 2) ? 1 : 0;
        s |= (flags.get(OPERATIONAL + 1) ? 1 : 0) << 1;
        s |= (flags.get(OPERATIONAL) ? 1 : 0) << 2;
        builder.setOperational(OperationalStatus.forValue(s));
    }

    @Override
    protected BitArray serializeFlags(final Lsp specObj) {
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(DELEGATE, specObj.isDelegate());
        flags.set(REMOVE, specObj.isRemove());
        flags.set(SYNC, specObj.isSync());
        flags.set(ADMINISTRATIVE, specObj.isAdministrative());
        if (specObj.getAugmentation(Lsp1.class) != null) {
            flags.set(CREATE_FLAG_OFFSET, specObj.getAugmentation(Lsp1.class).isCreate());
        }
        return flags;
    }
}
