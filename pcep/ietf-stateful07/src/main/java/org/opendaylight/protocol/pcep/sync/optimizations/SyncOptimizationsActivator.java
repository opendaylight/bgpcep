/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.sync.optimizations;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.speaker.entity.id.tlv.SpeakerEntityId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;

public class SyncOptimizationsActivator extends AbstractPCEPExtensionProviderActivator {

    @Override
    protected List<AutoCloseable> startImpl(final PCEPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();

        final TlvRegistry tlvReg = context.getTlvHandlerRegistry();
        final VendorInformationTlvRegistry viTlvReg = context.getVendorInformationTlvRegistry();
        regs.add(context.registerObjectParser(SyncOptimizationsLspObjectParser.CLASS, SyncOptimizationsLspObjectParser.TYPE,
            new SyncOptimizationsLspObjectParser(tlvReg, viTlvReg)));
        regs.add(context.registerObjectSerializer(Lsp.class, new SyncOptimizationsLspObjectParser(tlvReg, viTlvReg)));

        regs.add(context.registerObjectParser(SyncOptimizationsOpenObjectParser.CLASS, SyncOptimizationsOpenObjectParser.TYPE,
            new SyncOptimizationsOpenObjectParser(tlvReg, viTlvReg)));
        regs.add(context.registerObjectSerializer(Open.class, new SyncOptimizationsOpenObjectParser(tlvReg, viTlvReg)));

        regs.add(context.registerTlvParser(LspDbVersionTlvParser.TYPE, new LspDbVersionTlvParser()));
        regs.add(context.registerTlvSerializer(LspDbVersion.class, new LspDbVersionTlvParser()));

        regs.add(context.registerTlvParser(SpeakerEntityIdTlvParser.TYPE, new SpeakerEntityIdTlvParser()));
        regs.add(context.registerTlvSerializer(SpeakerEntityId.class, new SpeakerEntityIdTlvParser()));

        regs.add(context.registerTlvParser(SyncOptimizationsCapabilityTlvParser.TYPE, new SyncOptimizationsCapabilityTlvParser()));
        regs.add(context.registerTlvSerializer(Stateful.class, new SyncOptimizationsCapabilityTlvParser()));

        return regs;
    }
}
