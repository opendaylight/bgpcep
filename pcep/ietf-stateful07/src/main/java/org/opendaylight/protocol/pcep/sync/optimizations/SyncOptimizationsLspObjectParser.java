/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.sync.optimizations;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.pcep.ietf.initiated00.CInitiated00LspObjectParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.LspDbVersionTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;

public class SyncOptimizationsLspObjectParser extends CInitiated00LspObjectParser {

    public SyncOptimizationsLspObjectParser(final TlvRegistry tlvReg, final VendorInformationTlvRegistry viTlvReg) {
        super(tlvReg, viTlvReg);
    }

    @Override
    public void serializeTlvs(final Tlvs tlvs, final ByteBuf body) {
        super.serializeTlvs(tlvs, body);
        serializeAugmentation(tlvs.getAugmentation(Tlvs1.class), body);
    }

    private void serializeAugmentation(final Tlvs1 tlv, final ByteBuf body) {
        Preconditions.checkNotNull(tlv, "TLV object cannot be null.");
        Preconditions.checkArgument(tlv instanceof LspDbVersionTlv, "TLV object is not instance of LspDbVersionTlv.");
        final LspDbVersionTlv dbVersion = tlv;
        serializeTlv(dbVersion.getLspDbVersion(), body);
    }

    @Override
    public Lsp parseObject(final ObjectHeader header, final ByteBuf bytes) throws PCEPDeserializerException {
        return super.parseObject(header, bytes);
    }
}
