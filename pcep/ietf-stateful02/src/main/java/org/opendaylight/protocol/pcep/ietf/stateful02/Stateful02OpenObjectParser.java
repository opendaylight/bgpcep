/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful02;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.impl.object.PCEPOpenObjectParser;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Tlvs2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Tlvs2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.node.identifier.tlv.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

/**
 * Parser for Open Object
 */
@Deprecated
public class Stateful02OpenObjectParser extends PCEPOpenObjectParser {

    public Stateful02OpenObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
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
            if (t.getNodeIdentifier() != null) {
                statefulBuilder.setNodeIdentifier(t.getNodeIdentifier());
            }
        }
        if (tlv instanceof Stateful) {
            statefulBuilder.setStateful((Stateful) tlv);
        } else if (tlv instanceof NodeIdentifier) {
            statefulBuilder.setNodeIdentifier((NodeIdentifier) tlv);
        }
        tbuilder.addAugmentation(Tlvs2.class, statefulBuilder.build());
    }

    @Override
    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        super.serializeTlvs(tlvs, body);
        if (tlvs.getAugmentation(Tlvs2.class) != null) {
            final Tlvs2 statefulTlvs = tlvs.getAugmentation(Tlvs2.class);
            if (statefulTlvs.getStateful() != null) {
                serializeTlv(statefulTlvs.getStateful(), body);
            }
            if (statefulTlvs.getNodeIdentifier() != null) {
                serializeTlv(statefulTlvs.getNodeIdentifier(), body);
            }
        }
    }
}
