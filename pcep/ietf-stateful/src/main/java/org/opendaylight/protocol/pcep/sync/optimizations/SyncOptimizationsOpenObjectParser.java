/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.sync.optimizations;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.ietf.stateful.StatefulOpenObjectParser;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.speaker.entity.id.tlv.SpeakerEntityId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.Tlv;

public class SyncOptimizationsOpenObjectParser extends StatefulOpenObjectParser {
    public SyncOptimizationsOpenObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg);
    }

    @Override
    public void addTlv(final TlvsBuilder tbuilder, final Tlv tlv) {
        super.addTlv(tbuilder, tlv);
        final var syncOptTlvsBuilder = new Tlvs3Builder();
        final var syncOptTlvs = tbuilder.augmentation(Tlvs3.class);
        if (syncOptTlvs != null) {
            syncOptTlvsBuilder.setLspDbVersion(syncOptTlvs.getLspDbVersion());
            syncOptTlvsBuilder.setSpeakerEntityId(syncOptTlvs.getSpeakerEntityId());
        }
        if (tlv instanceof LspDbVersion ldv) {
            syncOptTlvsBuilder.setLspDbVersion(ldv);
        }
        if (tlv instanceof SpeakerEntityId sei) {
            syncOptTlvsBuilder.setSpeakerEntityId(sei);
        }
        tbuilder.addAugmentation(syncOptTlvsBuilder.build());
    }

    @Override
    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        super.serializeTlvs(tlvs, body);

        final var syncOptTlvs = tlvs.augmentation(Tlvs3.class);
        if (syncOptTlvs != null) {
            serializeOptionalTlv(syncOptTlvs.getLspDbVersion(), body);
            serializeOptionalTlv(syncOptTlvs.getSpeakerEntityId(), body);
        }
    }
}
