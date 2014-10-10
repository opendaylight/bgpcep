/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.lsp.setup.type01;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.impl.object.PCEPRequestParameterObjectParser;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.PathSetupTypeTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.rp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.rp.TlvsBuilder;

public class PcepRpObjectWithPstTlvParser extends PCEPRequestParameterObjectParser {

    public PcepRpObjectWithPstTlvParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg);
    }

    @Override
    public void addTlv(final TlvsBuilder builder, final Tlv tlv) {
        super.addTlv(builder, tlv);
        final Tlvs1Builder tlvBuilder = new Tlvs1Builder();
        final Tlvs1 tlvs = builder.getAugmentation(Tlvs1.class);
        if (tlvs != null && tlvs.getPathSetupType() != null) {
            tlvBuilder.setPathSetupType(tlvs.getPathSetupType());
        }
        if (tlv instanceof PathSetupType) {
            tlvBuilder.setPathSetupType((PathSetupType) tlv);
        }
        builder.addAugmentation(Tlvs1.class, tlvBuilder.build()).build();
    }

    @Override
    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        super.serializeTlvs(tlvs, body);
        if (tlvs.getAugmentation(Tlvs1.class) != null) {
            serializePathSetupType(tlvs.getAugmentation(Tlvs1.class), body);
        } else if (tlvs.getAugmentation(Tlvs2.class) != null) {
            serializePathSetupType(tlvs.getAugmentation(Tlvs2.class), body);
        } else if (tlvs.getAugmentation(Tlvs3.class) != null) {
            serializePathSetupType(tlvs.getAugmentation(Tlvs3.class), body);
        } else if (tlvs.getAugmentation(Tlvs4.class) != null) {
            serializePathSetupType(tlvs.getAugmentation(Tlvs4.class), body);
        }
    }

    private void serializePathSetupType(final PathSetupTypeTlv pstTlv, final ByteBuf body) {
        if (pstTlv.getPathSetupType() != null) {
            serializeTlv(pstTlv.getPathSetupType(), body);
        }
    }
}
