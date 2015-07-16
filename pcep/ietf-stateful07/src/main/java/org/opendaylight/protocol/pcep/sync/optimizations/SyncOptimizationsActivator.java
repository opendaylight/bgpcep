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
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.speaker.entity.id.tlv.SpeakerEntityId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.Stateful;

public class SyncOptimizationsActivator extends AbstractPCEPExtensionProviderActivator {

    @Override
    protected List<AutoCloseable> startImpl(final PCEPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();

        regs.add(context.registerTlvParser(SyncOptimizationsCapabilityTlvParser.TYPE, new SyncOptimizationsCapabilityTlvParser()));
        regs.add(context.registerTlvSerializer(Stateful.class, new SyncOptimizationsCapabilityTlvParser()));

        regs.add(context.registerTlvParser(LspDbVersionTlvParser.TYPE, new LspDbVersionTlvParser()));
        regs.add(context.registerTlvSerializer(LspDbVersion.class, new LspDbVersionTlvParser()));

        regs.add(context.registerTlvParser(SpeakerEntityIdTlvParser.TYPE, new SpeakerEntityIdTlvParser()));
        regs.add(context.registerTlvSerializer(SpeakerEntityId.class, new SpeakerEntityIdTlvParser()));

        return regs;
    }
}
