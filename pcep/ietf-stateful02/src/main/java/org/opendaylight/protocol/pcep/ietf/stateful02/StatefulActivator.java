/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful02;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.node.identifier.tlv.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcrep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcreq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;

@Deprecated
public class StatefulActivator extends AbstractPCEPExtensionProviderActivator {
    @Override
    protected List<AutoCloseable> startImpl(final PCEPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();

        regs.add(context.registerMessageParser(Stateful02PCUpdateRequestMessageParser.TYPE,
                new Stateful02PCUpdateRequestMessageParser(context.getObjectHandlerRegistry())));
        regs.add(context.registerMessageSerializer(Pcupd.class,
                new Stateful02PCUpdateRequestMessageParser(context.getObjectHandlerRegistry())));

        regs.add(context.registerMessageParser(Stateful02PCReportMessageParser.TYPE,
                new Stateful02PCReportMessageParser(context.getObjectHandlerRegistry())));
        regs.add(context.registerMessageSerializer(Pcrpt.class, new Stateful02PCReportMessageParser(context.getObjectHandlerRegistry())));

        regs.add(context.registerMessageParser(Stateful02PCReplyMessageParser.TYPE,
                new Stateful02PCReplyMessageParser(context.getObjectHandlerRegistry(), context.getVendorInformationObjectRegistry())));
        regs.add(context.registerMessageSerializer(Pcrep.class, new Stateful02PCReplyMessageParser(context.getObjectHandlerRegistry(),
                context.getVendorInformationObjectRegistry())));

        regs.add(context.registerMessageParser(Stateful02PCRequestMessageParser.TYPE,
                new Stateful02PCRequestMessageParser(context.getObjectHandlerRegistry(),
                        context.getVendorInformationObjectRegistry())));
        regs.add(context.registerMessageSerializer(Pcreq.class, new Stateful02PCRequestMessageParser(context.getObjectHandlerRegistry(),
                context.getVendorInformationObjectRegistry())));

        regs.add(context.registerObjectParser(Stateful02LspObjectParser.CLASS, Stateful02LspObjectParser.TYPE,
                new Stateful02LspObjectParser(context.getTlvHandlerRegistry(), context.getVendorInformationTlvRegistry())));
        regs.add(context.registerObjectSerializer(Lsp.class, new Stateful02LspObjectParser(context.getTlvHandlerRegistry(),
                context.getVendorInformationTlvRegistry())));

        regs.add(context.registerObjectParser(Stateful02OpenObjectParser.CLASS, Stateful02OpenObjectParser.TYPE,
                new Stateful02OpenObjectParser(context.getTlvHandlerRegistry(), context.getVendorInformationTlvRegistry())));
        regs.add(context.registerObjectSerializer(Open.class, new Stateful02OpenObjectParser(context.getTlvHandlerRegistry(),
                context.getVendorInformationTlvRegistry())));

        regs.add(context.registerObjectParser(Stateful02LspaObjectParser.CLASS, Stateful02LspaObjectParser.TYPE,
                new Stateful02LspaObjectParser(context.getTlvHandlerRegistry(),
                        context.getVendorInformationTlvRegistry())));
        regs.add(context.registerObjectSerializer(Lspa.class, new Stateful02LspaObjectParser(context.getTlvHandlerRegistry(),
                context.getVendorInformationTlvRegistry())));

        regs.add(context.registerTlvParser(Stateful02StatefulCapabilityTlvParser.TYPE, new Stateful02StatefulCapabilityTlvParser()));
        regs.add(context.registerTlvSerializer(Stateful.class, new Stateful02StatefulCapabilityTlvParser()));

        regs.add(context.registerTlvParser(Stateful02LspDbVersionTlvParser.TYPE, new Stateful02LspDbVersionTlvParser()));
        regs.add(context.registerTlvSerializer(LspDbVersion.class, new Stateful02LspDbVersionTlvParser()));

        regs.add(context.registerTlvParser(Stateful02NodeIdentifierTlvParser.TYPE, new Stateful02NodeIdentifierTlvParser()));
        regs.add(context.registerTlvSerializer(NodeIdentifier.class, new Stateful02NodeIdentifierTlvParser()));

        regs.add(context.registerTlvParser(Stateful02LspSymbolicNameTlvParser.TYPE, new Stateful02LspSymbolicNameTlvParser()));
        regs.add(context.registerTlvSerializer(SymbolicPathName.class, new Stateful02LspSymbolicNameTlvParser()));

        regs.add(context.registerTlvParser(Stateful02RSVPErrorSpecTlvParser.TYPE, new Stateful02RSVPErrorSpecTlvParser()));
        regs.add(context.registerTlvSerializer(RsvpErrorSpec.class, new Stateful02RSVPErrorSpecTlvParser()));

        return regs;
    }
}
