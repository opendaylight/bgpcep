/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.initiated00;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.stateful.capability.tlv.Stateful;

public final class CrabbeInitiatedActivator extends AbstractPCEPExtensionProviderActivator {
    @Override
    protected List<AutoCloseable> startImpl(final PCEPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();

        regs.add(context.registerMessageParser(CInitiated00PCInitiateMessageParser.TYPE,
                new CInitiated00PCInitiateMessageParser(context.getObjectHandlerRegistry())));
        regs.add(context.registerMessageSerializer(Pcinitiate.class,
                new CInitiated00PCInitiateMessageParser(context.getObjectHandlerRegistry())));

        final TlvRegistry tlvReg = context.getTlvHandlerRegistry();
        final VendorInformationTlvRegistry viTlvReg = context.getVendorInformationTlvRegistry();
        regs.add(context.registerObjectParser(CInitiated00LspObjectParser.CLASS, CInitiated00LspObjectParser.TYPE,
                new CInitiated00LspObjectParser(tlvReg, viTlvReg)));
        regs.add(context.registerObjectSerializer(Lsp.class, new CInitiated00LspObjectParser(tlvReg, viTlvReg)));
        regs.add(context.registerObjectParser(CInitiated00SrpObjectParser.CLASS, CInitiated00SrpObjectParser.TYPE,
                new CInitiated00SrpObjectParser(tlvReg, viTlvReg)));
        regs.add(context.registerObjectSerializer(Srp.class, new CInitiated00SrpObjectParser(tlvReg, viTlvReg)));

        regs.add(context.registerTlvParser(CInitiated00StatefulCapabilityTlvParser.TYPE, new CInitiated00StatefulCapabilityTlvParser()));
        regs.add(context.registerTlvSerializer(Stateful.class, new CInitiated00StatefulCapabilityTlvParser()));

        return regs;
    }
}
