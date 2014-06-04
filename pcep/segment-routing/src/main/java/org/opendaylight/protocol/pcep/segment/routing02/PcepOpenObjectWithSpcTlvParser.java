/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing02;

import org.opendaylight.protocol.pcep.impl.object.PCEPOpenObjectParser;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing._02.rev140506.sr.pce.capability.tlv.SrPceCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public class PcepOpenObjectWithSpcTlvParser extends PCEPOpenObjectParser {

    public PcepOpenObjectWithSpcTlvParser(TlvRegistry tlvReg) {
        super(tlvReg);
    }

    @Override
    public void addTlv(TlvsBuilder tbuilder, Tlv tlv) {
        super.addTlv(tbuilder, tlv);
        final Tlvs1Builder tlvBuilder = new Tlvs1Builder();
        if (tbuilder.getAugmentation(Tlvs1.class) != null) {
            final Tlvs1 tlvs = tbuilder.getAugmentation(Tlvs1.class);
            if (tlvs.getSrPceCapability() != null) {
                tlvBuilder.setSrPceCapability(tlvs.getSrPceCapability());
            }
        }
        if (tlv instanceof SrPceCapability) {
            tlvBuilder.setSrPceCapability((SrPceCapability) tlv);
        }
        tbuilder.addAugmentation(Tlvs1.class, tlvBuilder.build());
    }

    @Override
    public byte[] serializeTlvs(Tlvs tlvs) {
        if (tlvs == null) {
            return new byte[0];
        }
        final byte[] prev = super.serializeTlvs(tlvs);
        int finalLength = prev.length;
        byte[] nameBytes = null;
        if (tlvs.getAugmentation(Tlvs1.class) != null) {
            final Tlvs1 spcTlvs = tlvs.getAugmentation(Tlvs1.class);
            if (spcTlvs.getSrPceCapability() != null) {
                nameBytes = serializeTlv(spcTlvs.getSrPceCapability());
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
