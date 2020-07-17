/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.initiated;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.initiated.rev181109.Pcinitiate;
import org.opendaylight.yangtools.concepts.Registration;

public final class InitiatedActivator extends AbstractPCEPExtensionProviderActivator {
    @Override
    protected List<Registration> startImpl(final PCEPExtensionProviderContext context) {
        final List<Registration> regs = new ArrayList<>();

        regs.add(context.registerMessageParser(InitiatedPCInitiateMessageParser.TYPE,
                new InitiatedPCInitiateMessageParser(context.getObjectHandlerRegistry())));
        regs.add(context.registerMessageSerializer(Pcinitiate.class,
                new InitiatedPCInitiateMessageParser(context.getObjectHandlerRegistry())));

        final TlvRegistry tlvReg = context.getTlvHandlerRegistry();
        final VendorInformationTlvRegistry viTlvReg = context.getVendorInformationTlvRegistry();
        regs.add(context.registerObjectParser(new InitiatedLspObjectParser(tlvReg, viTlvReg)));
        regs.add(context.registerObjectSerializer(Lsp.class, new InitiatedLspObjectParser(tlvReg, viTlvReg)));
        regs.add(context.registerObjectParser(new InitiatedSrpObjectParser(tlvReg, viTlvReg)));
        regs.add(context.registerObjectSerializer(Srp.class, new InitiatedSrpObjectParser(tlvReg, viTlvReg)));

        regs.add(context.registerTlvParser(InitiatedStatefulCapabilityTlvParser.TYPE,
            new InitiatedStatefulCapabilityTlvParser()));
        regs.add(context.registerTlvSerializer(Stateful.class, new InitiatedStatefulCapabilityTlvParser()));

        return regs;
    }
}
