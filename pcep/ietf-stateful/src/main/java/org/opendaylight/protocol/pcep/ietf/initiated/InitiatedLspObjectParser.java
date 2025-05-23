/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.initiated;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.ietf.stateful.StatefulLspObjectParser;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Lsp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Lsp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.object.LspBuilder;

/**
 * Parser for {@link Lsp}.
 */
public class InitiatedLspObjectParser extends StatefulLspObjectParser {

    private static final int CREATE_FLAG_OFFSET = 4;

    public InitiatedLspObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg);
    }

    @Override
    protected void parseFlags(final LspBuilder builder, final ByteBuf bytes) {
        final BitArray flags = BitArray.valueOf(bytes, FLAGS_SIZE);
        builder.setPceAllocation(flags.get(PCE_ALLOCATION));
        builder.setDelegate(flags.get(DELEGATE));
        builder.setSync(flags.get(SYNC));
        builder.setRemove(flags.get(REMOVE));
        builder.setAdministrative(flags.get(ADMINISTRATIVE));
        builder.addAugmentation(new Lsp1Builder().setCreate(flags.get(CREATE_FLAG_OFFSET)).build());
        short oper = 0;
        oper |= flags.get(OPERATIONAL + 2) ? 1 : 0;
        oper |= (flags.get(OPERATIONAL + 1) ? 1 : 0) << 1;
        oper |= (flags.get(OPERATIONAL) ? 1 : 0) << 2;
        builder.setOperational(OperationalStatus.forValue(oper));
    }

    @Override
    protected BitArray serializeFlags(final Lsp specObj) {
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(PCE_ALLOCATION, specObj.getPceAllocation());
        flags.set(DELEGATE, specObj.getDelegate());
        flags.set(REMOVE, specObj.getRemove());
        flags.set(SYNC, specObj.getSync());
        flags.set(ADMINISTRATIVE, specObj.getAdministrative());
        final Lsp1 aug = specObj.augmentation(Lsp1.class);
        if (aug != null) {
            flags.set(CREATE_FLAG_OFFSET, aug.getCreate());
        }
        return flags;
    }
}
