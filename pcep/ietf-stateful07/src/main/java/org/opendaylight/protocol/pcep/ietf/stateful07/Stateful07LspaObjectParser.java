/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.parser.object.PCEPLspaObjectParser;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.lspa.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.lspa.TlvsBuilder;

/**
 * Parser for Lspa object
 */
public class Stateful07LspaObjectParser extends PCEPLspaObjectParser {

    public Stateful07LspaObjectParser(TlvRegistry tlvReg, VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg);
    }

    @Override
    public void addTlv(final TlvsBuilder tbuilder, final Tlv tlv) {
        super.addTlv(tbuilder, tlv);
        final Tlvs2Builder nameBuilder = new Tlvs2Builder();
        if (tbuilder.getAugmentation(Tlvs2.class) != null) {
            final Tlvs2 t = tbuilder.getAugmentation(Tlvs2.class);
            if (t.getSymbolicPathName() != null) {
                nameBuilder.setSymbolicPathName(t.getSymbolicPathName());
            }
        }
        if (tlv instanceof SymbolicPathName) {
            nameBuilder.setSymbolicPathName((SymbolicPathName) tlv);
        }
        tbuilder.addAugmentation(Tlvs2.class, nameBuilder.build());
    }

    @Override
    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        super.serializeTlvs(tlvs, body);
        if (tlvs.getAugmentation(Tlvs2.class) != null) {
            final Tlvs2 nameTlvs = tlvs.getAugmentation(Tlvs2.class);
            if (nameTlvs.getSymbolicPathName() != null) {
                serializeTlv(nameTlvs.getSymbolicPathName(), body);
            }
        }
    }
}
