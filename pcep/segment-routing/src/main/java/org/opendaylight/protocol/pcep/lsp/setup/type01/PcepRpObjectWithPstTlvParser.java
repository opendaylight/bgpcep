/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.lsp.setup.type01;

import org.opendaylight.protocol.pcep.impl.object.PCEPRequestParameterObjectParser;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.RpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.rp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.rp.TlvsBuilder;

public class PcepRpObjectWithPstTlvParser extends PCEPRequestParameterObjectParser {

    public PcepRpObjectWithPstTlvParser(TlvRegistry tlvReg) {
        super(tlvReg);
    }

    @Override
    public void addTlv(RpBuilder builder, Tlv tlv) {
        super.addTlv(builder, tlv);
        final Tlvs3Builder tlvBuilder = new Tlvs3Builder();
        if (builder.getTlvs() != null) {
            if (builder.getTlvs().getAugmentation(Tlvs3.class) != null) {
                final Tlvs3 t = builder.getTlvs().getAugmentation(Tlvs3.class);
                if (t.getPathSetupType() != null) {
                    tlvBuilder.setPathSetupType(t.getPathSetupType());
                }
            }
        }
        if (tlv instanceof PathSetupType) {
            tlvBuilder.setPathSetupType((PathSetupType) tlv);
        }
        builder.setTlvs(new TlvsBuilder().addAugmentation(Tlvs3.class, tlvBuilder.build()).build());
    }

    @Override
    public byte[] serializeTlvs(Tlvs tlvs) {
        if (tlvs == null) {
            return new byte[0];
        }
        final byte[] prev = super.serializeTlvs(tlvs);
        int finalLength = prev.length;
        byte[] nameBytes = null;
        if (tlvs.getAugmentation(Tlvs3.class) != null) {
            final Tlvs3 nameTlvs = tlvs.getAugmentation(Tlvs3.class);
            if (nameTlvs.getPathSetupType() != null) {
                nameBytes = serializeTlv(nameTlvs.getPathSetupType());
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
