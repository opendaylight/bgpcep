/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful02;

import org.opendaylight.protocol.pcep.impl.object.PCEPLspaObjectParser;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.lspa.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.lspa.TlvsBuilder;

/**
 * Parser for LSPA object
 */
public class Stateful02LspaObjectParser extends PCEPLspaObjectParser {

    public Stateful02LspaObjectParser(final TlvRegistry tlvReg) {
        super(tlvReg);
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
    public byte[] serializeTlvs(final Tlvs tlvs) {
        if (tlvs == null) {
            return new byte[0];
        }
        final byte[] prev = super.serializeTlvs(tlvs);
        int finalLength = prev.length;
        byte[] nameBytes = null;
        if (tlvs.getAugmentation(Tlvs2.class) != null) {
            final Tlvs2 nameTlvs = tlvs.getAugmentation(Tlvs2.class);
            if (nameTlvs.getSymbolicPathName() != null) {
                nameBytes = serializeTlv(nameTlvs.getSymbolicPathName());
                finalLength += nameBytes.length;
            }
        }
        final byte[] result = new byte[finalLength];
        ByteArray.copyWhole(prev, result, 0);
        int offset = prev.length;
        if (nameBytes != null) {
            ByteArray.copyWhole(nameBytes, result, offset);
            offset += nameBytes.length;
        }
        return result;
    }
}
