/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.stateful02;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.error.code.tlv.LspErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.symbolic.path.name.tlv.SymbolicPathName;

public class StatefulActivator extends AbstractPCEPExtensionProviderActivator {
	@Override
	protected List<AutoCloseable> startImpl(final PCEPExtensionProviderContext context) {
		final List<AutoCloseable> regs = new ArrayList<>();

		regs.add(context.registerMessageParser(PCEPUpdateRequestMessageParser.TYPE,
				new PCEPUpdateRequestMessageParser(context.getObjectHandlerRegistry())));
		regs.add(context.registerMessageSerializer(Pcupd.class, new PCEPUpdateRequestMessageParser(context.getObjectHandlerRegistry())));

		regs.add(context.registerMessageParser(PCEPReportMessageParser.TYPE,
				new PCEPReportMessageParser(context.getObjectHandlerRegistry())));
		regs.add(context.registerMessageSerializer(Pcrpt.class, new PCEPReportMessageParser(context.getObjectHandlerRegistry())));

		regs.add(context.registerObjectParser(PCEPLspObjectParser.TYPE, PCEPLspObjectParser.CLASS,
				new PCEPLspObjectParser(context.getTlvHandlerRegistry())));
		regs.add(context.registerObjectSerializer(Lsp.class, new PCEPLspObjectParser(context.getTlvHandlerRegistry())));

		regs.add(context.registerTlvParser(PCEStatefulCapabilityTlvParser.TYPE, new PCEStatefulCapabilityTlvParser()));
		regs.add(context.registerTlvSerializer(Stateful.class, new PCEStatefulCapabilityTlvParser()));

		regs.add(context.registerTlvParser(LspDbVersionTlvParser.TYPE, new LspDbVersionTlvParser()));
		regs.add(context.registerTlvSerializer(LspDbVersion.class, new LspDbVersionTlvParser()));

		regs.add(context.registerTlvParser(LspUpdateErrorTlvParser.TYPE, new LspUpdateErrorTlvParser()));
		regs.add(context.registerTlvSerializer(LspErrorCode.class, new LspUpdateErrorTlvParser()));

		regs.add(context.registerTlvParser(LspSymbolicNameTlvParser.TYPE, new LspSymbolicNameTlvParser()));
		regs.add(context.registerTlvSerializer(SymbolicPathName.class, new LspSymbolicNameTlvParser()));

		regs.add(context.registerTlvParser(LspIdentifierIpv4TlvParser.TYPE, new LspIdentifierIpv4TlvParser()));
		regs.add(context.registerTlvSerializer(LspIdentifiers.class, new LspIdentifierIpv4TlvParser()));

		regs.add(context.registerTlvParser(LspIdentifierIpv6TlvParser.TYPE, new LspIdentifierIpv6TlvParser()));

		regs.add(context.registerTlvParser(RSVPErrorSpecTlvParser.TYPE, new RSVPErrorSpecTlvParser()));
		regs.add(context.registerTlvSerializer(RsvpErrorSpec.class, new RSVPErrorSpecTlvParser()));

		return regs;
	}
}
