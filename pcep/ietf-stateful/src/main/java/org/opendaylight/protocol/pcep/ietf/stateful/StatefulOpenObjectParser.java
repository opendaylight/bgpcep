/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.parser.object.PCEPOpenObjectParser;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.Tlv;

/**
 * Parser for Open object.
 */
public class StatefulOpenObjectParser extends PCEPOpenObjectParser {
    public StatefulOpenObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg);
    }

    @Override
    public void addTlv(final TlvsBuilder tbuilder, final Tlv tlv) {
        super.addTlv(tbuilder, tlv);

        final var statefulBuilder = new Tlvs1Builder();
        final var statefulTlvs = tbuilder.augmentation(Tlvs1.class);
        if (statefulTlvs != null) {
            statefulBuilder.setStateful(statefulTlvs.getStateful());
        }
        if (tlv instanceof Stateful stateful) {
            statefulBuilder.setStateful(stateful);
        }
        tbuilder.addAugmentation(statefulBuilder.build());
    }

    @Override
    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        super.serializeTlvs(tlvs, body);

        serializeOptionalTlv(tlvs.getOfList(), body);
        final var statefulTlvs = tlvs.augmentation(Tlvs1.class);
        if (statefulTlvs != null) {
            serializeOptionalTlv(statefulTlvs.getStateful(), body);
        }
    }
}
