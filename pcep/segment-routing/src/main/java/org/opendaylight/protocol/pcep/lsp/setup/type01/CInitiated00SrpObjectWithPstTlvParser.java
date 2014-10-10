/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.lsp.setup.type01;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.ietf.initiated00.CInitiated00SrpObjectParser;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.srp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.srp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.PathSetupTypeTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs7;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs7Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs8;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

public class CInitiated00SrpObjectWithPstTlvParser extends CInitiated00SrpObjectParser {

    public CInitiated00SrpObjectWithPstTlvParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg);
    }

    @Override
    public void addTlv(final TlvsBuilder builder, final Tlv tlv) {
        super.addTlv(builder, tlv);
        final Tlvs7Builder tlvBuilder = new Tlvs7Builder();
        final Tlvs7 tlvs = builder.getAugmentation(Tlvs7.class);
        if (tlvs != null && tlvs.getPathSetupType() != null) {
            tlvBuilder.setPathSetupType(tlvs.getPathSetupType());
        }
        if (tlv instanceof PathSetupType) {
            tlvBuilder.setPathSetupType((PathSetupType) tlv);
        }
        builder.addAugmentation(Tlvs7.class, tlvBuilder.build());
    }

    @Override
    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        super.serializeTlvs(tlvs, body);
        if (tlvs.getAugmentation(Tlvs5.class) != null) {
            serializePathSetupType(tlvs.getAugmentation(Tlvs5.class), body);
        } else if (tlvs.getAugmentation(Tlvs6.class) != null) {
            serializePathSetupType(tlvs.getAugmentation(Tlvs6.class), body);
        } else if (tlvs.getAugmentation(Tlvs7.class) != null) {
            serializePathSetupType(tlvs.getAugmentation(Tlvs7.class), body);
        } else if (tlvs.getAugmentation(Tlvs8.class) != null) {
            serializePathSetupType(tlvs.getAugmentation(Tlvs8.class), body);
        }
    }

    private void serializePathSetupType(final PathSetupTypeTlv pstTlv, final ByteBuf body) {
        if (pstTlv.getPathSetupType() != null) {
            serializeTlv(pstTlv.getPathSetupType(), body);
        }
    }
}
