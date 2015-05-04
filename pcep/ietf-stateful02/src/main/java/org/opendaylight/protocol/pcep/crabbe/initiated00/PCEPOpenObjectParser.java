/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.crabbe.initiated00;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02OpenObjectParser;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.lsp.cleanup.tlv.LspCleanup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Tlvs2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Tlvs2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

/**
 * Parser for Open object
 */
@Deprecated
public final class PCEPOpenObjectParser extends Stateful02OpenObjectParser {

    public PCEPOpenObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg);
    }

    @Override
    public void addTlv(final TlvsBuilder tbuilder, final Tlv tlv) {
        super.addTlv(tbuilder, tlv);
        final Tlvs2Builder statefulBuilder = new Tlvs2Builder();
        if (tbuilder.getAugmentation(Tlvs2.class) != null) {
            final Tlvs2 t = tbuilder.getAugmentation(Tlvs2.class);
            if (t.getStateful() != null) {
                statefulBuilder.setStateful(t.getStateful());
            }
        }
        final Tlvs1Builder cleanupBuilder = new Tlvs1Builder();
        if (tbuilder.getAugmentation(Tlvs1.class) != null) {
            final Tlvs1 t = tbuilder.getAugmentation(Tlvs1.class);
            if (t.getLspCleanup() != null) {
                cleanupBuilder.setLspCleanup(t.getLspCleanup());
            }
        }
        if (tlv instanceof Stateful) {
            statefulBuilder.setStateful((Stateful) tlv);
        } else if (tlv instanceof LspCleanup) {
            cleanupBuilder.setLspCleanup((LspCleanup) tlv);
        }
        tbuilder.addAugmentation(Tlvs2.class, statefulBuilder.build());
        tbuilder.addAugmentation(Tlvs1.class, cleanupBuilder.build());
    }

    @Override
    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        super.serializeTlvs(tlvs, body);
        if (tlvs.getAugmentation(Tlvs1.class) != null) {
            final Tlvs1 cleanupTlv = tlvs.getAugmentation(Tlvs1.class);
            if (cleanupTlv.getLspCleanup() != null) {
                serializeTlv(cleanupTlv.getLspCleanup(), body);
            }
        }
    }
}
