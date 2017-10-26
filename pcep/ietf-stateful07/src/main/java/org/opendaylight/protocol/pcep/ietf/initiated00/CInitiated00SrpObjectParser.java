/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.initiated00;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07SrpObjectParser;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Srp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Srp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.SrpBuilder;

/**
 * Parser for {@link Srp}
 */
public class CInitiated00SrpObjectParser extends Stateful07SrpObjectParser {

    private static final int REMOVE_FLAG = 31;

    public CInitiated00SrpObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg);
    }

    @Override
    protected void parseFlags(final SrpBuilder builder, final ByteBuf bytes) {
        final BitArray flags = BitArray.valueOf(bytes, FLAGS_SIZE);
        builder.addAugmentation(Srp1.class, new Srp1Builder().setRemove(flags.get(REMOVE_FLAG)).build());
    }

    @Override
    protected void serializeFlags(final Srp srp, final ByteBuf body) {
        final BitArray flags = new BitArray(FLAGS_SIZE);
        if (srp.getAugmentation(Srp1.class) != null) {
            flags.set(REMOVE_FLAG, srp.getAugmentation(Srp1.class).isRemove());
        }
        flags.toByteBuf(body);
    }
}
