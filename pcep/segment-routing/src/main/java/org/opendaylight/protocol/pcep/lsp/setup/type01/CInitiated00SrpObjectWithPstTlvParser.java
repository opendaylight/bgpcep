/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.lsp.setup.type01;

import org.opendaylight.protocol.pcep.ietf.initiated00.CInitiated00SrpObjectParser;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.srp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.srp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs8;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs8Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

public class CInitiated00SrpObjectWithPstTlvParser extends CInitiated00SrpObjectParser {

    public CInitiated00SrpObjectWithPstTlvParser(TlvRegistry tlvReg) {
        super(tlvReg);
    }

    @Override
    public void addTlv(SrpBuilder builder, Tlv tlv) {
        super.addTlv(builder, tlv);
        final Tlvs8Builder tlvBuilder = new Tlvs8Builder();
        if (builder.getTlvs() != null) {
            if (builder.getTlvs().getAugmentation(Tlvs8.class) != null) {
                final Tlvs8 t = builder.getTlvs().getAugmentation(Tlvs8.class);
                if (t.getPathSetupType() != null) {
                    tlvBuilder.setPathSetupType(t.getPathSetupType());
                }
            }
        }
        if (tlv instanceof PathSetupType) {
            tlvBuilder.setPathSetupType((PathSetupType) tlv);
        }
        builder.setTlvs(new TlvsBuilder().addAugmentation(Tlvs8.class, tlvBuilder.build()).build());
    }

    @Override
    public byte[] serializeTlvs(Tlvs tlvs) {
        if (tlvs == null) {
            return new byte[0];
        }
        final byte[] prev = super.serializeTlvs(tlvs);
        int finalLength = prev.length;
        byte[] nameBytes = null;
        if (tlvs.getAugmentation(Tlvs8.class) != null) {
            final Tlvs8 nameTlvs = tlvs.getAugmentation(Tlvs8.class);
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
