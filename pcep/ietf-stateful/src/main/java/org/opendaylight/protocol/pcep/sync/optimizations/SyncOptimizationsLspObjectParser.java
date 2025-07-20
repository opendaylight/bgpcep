/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.sync.optimizations;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.ietf.initiated.InitiatedLspObjectParser;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.object.lsp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.Tlv;

public class SyncOptimizationsLspObjectParser extends InitiatedLspObjectParser {
    public SyncOptimizationsLspObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg);
    }

    @Override
    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        if (tlvs == null) {
            return;
        }
        super.serializeTlvs(tlvs, body);

        final var syncOptTlvs = tlvs.augmentation(Tlvs1.class);
        if (syncOptTlvs != null) {
            serializeTlv(syncOptTlvs.getLspDbVersion(), body);
        }
    }

    @Override
    public void addTlv(final TlvsBuilder tbuilder, final Tlv tlv) {
        super.addTlv(tbuilder, tlv);

        final var syncOptTlvsBuilder = new Tlvs1Builder();
        final var syncOptTlvs = tbuilder.augmentation(Tlvs1.class);
        if (syncOptTlvs != null) {
            syncOptTlvsBuilder.setLspDbVersion(syncOptTlvs.getLspDbVersion());
        }
        if (tlv instanceof LspDbVersion ldv) {
            syncOptTlvsBuilder.setLspDbVersion(ldv);
        }
        tbuilder.addAugmentation(syncOptTlvsBuilder.build());
    }
}
