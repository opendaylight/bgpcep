/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.error.code.tlv.LspErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.path.binding.tlv.PathBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;

public final class StatefulActivator extends AbstractPCEPExtensionProviderActivator {
    @Override
    protected List<AutoCloseable> startImpl(final PCEPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();

        final ObjectRegistry objReg = context.getObjectHandlerRegistry();
        regs.add(context.registerMessageParser(Stateful07PCUpdateRequestMessageParser.TYPE,
            new Stateful07PCUpdateRequestMessageParser(objReg)));
        regs.add(context.registerMessageSerializer(Pcupd.class, new Stateful07PCUpdateRequestMessageParser(objReg)));
        regs.add(context.registerMessageParser(Stateful07PCReportMessageParser.TYPE, new Stateful07PCReportMessageParser(objReg)));
        regs.add(context.registerMessageSerializer(Pcrpt.class, new Stateful07PCReportMessageParser(objReg)));
        regs.add(context.registerMessageParser(Stateful07ErrorMessageParser.TYPE, new Stateful07ErrorMessageParser(objReg)));
        regs.add(context.registerMessageSerializer(Pcerr.class, new Stateful07ErrorMessageParser(objReg)));

        final TlvRegistry tlvReg = context.getTlvHandlerRegistry();
        final VendorInformationTlvRegistry viTlvReg = context.getVendorInformationTlvRegistry();
        regs.add(context.registerObjectParser(Stateful07LspObjectParser.CLASS, Stateful07LspObjectParser.TYPE,
            new Stateful07LspObjectParser(tlvReg, viTlvReg)));
        regs.add(context.registerObjectSerializer(Lsp.class, new Stateful07LspObjectParser(tlvReg, viTlvReg)));
        regs.add(context.registerObjectParser(Stateful07SrpObjectParser.CLASS, Stateful07SrpObjectParser.TYPE,
            new Stateful07SrpObjectParser(tlvReg, viTlvReg)));
        regs.add(context.registerObjectSerializer(Srp.class, new Stateful07SrpObjectParser(tlvReg, viTlvReg)));
        regs.add(context.registerObjectParser(Stateful07OpenObjectParser.CLASS, Stateful07OpenObjectParser.TYPE,
            new Stateful07OpenObjectParser(tlvReg, viTlvReg)));
        regs.add(context.registerObjectSerializer(Open.class, new Stateful07OpenObjectParser(tlvReg, viTlvReg)));

        regs.add(context.registerTlvParser(Stateful07LSPIdentifierIpv4TlvParser.TYPE, new Stateful07LSPIdentifierIpv4TlvParser()));
        regs.add(context.registerTlvParser(Stateful07LSPIdentifierIpv6TlvParser.TYPE, new Stateful07LSPIdentifierIpv6TlvParser()));
        regs.add(context.registerTlvSerializer(LspIdentifiers.class, new Stateful07LSPIdentifierIpv4TlvParser()));
        regs.add(context.registerTlvParser(Stateful07LspUpdateErrorTlvParser.TYPE, new Stateful07LspUpdateErrorTlvParser()));
        regs.add(context.registerTlvSerializer(LspErrorCode.class, new Stateful07LspUpdateErrorTlvParser()));
        regs.add(context.registerTlvParser(Stateful07RSVPErrorSpecTlvParser.TYPE, new Stateful07RSVPErrorSpecTlvParser()));
        regs.add(context.registerTlvSerializer(RsvpErrorSpec.class, new Stateful07RSVPErrorSpecTlvParser()));
        regs.add(context.registerTlvParser(Stateful07StatefulCapabilityTlvParser.TYPE, new Stateful07StatefulCapabilityTlvParser()));
        regs.add(context.registerTlvSerializer(Stateful.class, new Stateful07StatefulCapabilityTlvParser()));
        regs.add(context.registerTlvParser(Stateful07LspSymbolicNameTlvParser.TYPE, new Stateful07LspSymbolicNameTlvParser()));
        regs.add(context.registerTlvSerializer(SymbolicPathName.class, new Stateful07LspSymbolicNameTlvParser()));
        regs.add(context.registerTlvParser(PathBindingTlvParser.TYPE, new PathBindingTlvParser()));
        regs.add(context.registerTlvSerializer(PathBinding.class, new PathBindingTlvParser()));
        return regs;
    }
}
