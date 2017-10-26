/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.sync.optimizations;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07OpenObjectParser;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.speaker.entity.id.tlv.SpeakerEntityId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public class SyncOptimizationsOpenObjectParser extends Stateful07OpenObjectParser {

    public SyncOptimizationsOpenObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg);
    }


    @Override
    public void addTlv(final TlvsBuilder tbuilder, final Tlv tlv) {
        super.addTlv(tbuilder, tlv);
        final Tlvs3Builder syncOptTlvsBuilder = new Tlvs3Builder();
        if (tbuilder.getAugmentation(Tlvs3.class) != null) {
            final Tlvs3 t = tbuilder.getAugmentation(Tlvs3.class);
            if (t.getLspDbVersion() != null) {
                syncOptTlvsBuilder.setLspDbVersion(t.getLspDbVersion());
            }
            if (t.getSpeakerEntityId() != null) {
                syncOptTlvsBuilder.setSpeakerEntityId(t.getSpeakerEntityId());
            }
        }
        if (tlv instanceof LspDbVersion) {
            syncOptTlvsBuilder.setLspDbVersion((LspDbVersion) tlv);
        }
        if (tlv instanceof SpeakerEntityId) {
            syncOptTlvsBuilder.setSpeakerEntityId((SpeakerEntityId) tlv);
        }
        tbuilder.addAugmentation(Tlvs3.class, syncOptTlvsBuilder.build());
    }

    @Override
    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        super.serializeTlvs(tlvs, body);
        if (tlvs.getAugmentation(Tlvs3.class) != null) {
            final Tlvs3 syncOptTlvs = tlvs.getAugmentation(Tlvs3.class);
            if (syncOptTlvs.getLspDbVersion() != null) {
                serializeTlv(syncOptTlvs.getLspDbVersion(), body);
            }
            if (syncOptTlvs.getSpeakerEntityId() != null) {
                serializeTlv(syncOptTlvs.getSpeakerEntityId(), body);
            }
        }
    }
}
