/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.crabbe.initiated00;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.lsp.cleanup.tlv.LspCleanup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;

@Deprecated
public class InitiatedActivator extends AbstractPCEPExtensionProviderActivator {
    @Override
    protected List<AutoCloseable> startImpl(final PCEPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();

        regs.add(context.registerMessageParser(PcinitiateMessageParser.TYPE,
                new PcinitiateMessageParser(context.getObjectHandlerRegistry())));
        regs.add(context.registerMessageSerializer(Pcinitiate.class, new PcinitiateMessageParser(context.getObjectHandlerRegistry())));

        regs.add(context.registerObjectParser(PCEPOpenObjectParser.CLASS, PCEPOpenObjectParser.TYPE,
                new PCEPOpenObjectParser(context.getTlvHandlerRegistry(), context.getVendorInformationTlvRegistry())));
        regs.add(context.registerObjectSerializer(Open.class, new PCEPOpenObjectParser(context.getTlvHandlerRegistry(),
                context.getVendorInformationTlvRegistry())));

        regs.add(context.registerTlvParser(LSPCleanupTlvParser.TYPE, new LSPCleanupTlvParser()));
        regs.add(context.registerTlvSerializer(LspCleanup.class, new LSPCleanupTlvParser()));

        regs.add(context.registerTlvParser(PCEStatefulCapabilityTlvParser.TYPE, new PCEStatefulCapabilityTlvParser()));
        regs.add(context.registerTlvSerializer(Stateful.class, new PCEStatefulCapabilityTlvParser()));

        return regs;
    }
}
