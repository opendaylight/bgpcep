/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.pcep.impl.message.PCEPReplyMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPReportMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPRequestMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPUpdateRequestMessageParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPLspObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPOpenObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPSrpObjectParser;
import org.opendaylight.protocol.pcep.impl.tlv.LSPIdentifierIpv4TlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LSPIdentifierIpv6TlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LspSymbolicNameTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LspUpdateErrorTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.PCEStatefulCapabilityTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.RSVPErrorSpecTlvParser;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.error.code.tlv.LspErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcrep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcreq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;

public final class StatefulActivator extends AbstractPCEPExtensionProviderActivator {
	@Override
	protected List<AutoCloseable> startImpl(final PCEPExtensionProviderContext context) {
		final List<AutoCloseable> regs = new ArrayList<>();

		final ObjectHandlerRegistry objReg = context.getObjectHandlerRegistry();
		regs.add(context.registerMessageParser(PCEPUpdateRequestMessageParser.TYPE, new PCEPUpdateRequestMessageParser(objReg)));
		regs.add(context.registerMessageSerializer(Pcupd.class, new PCEPUpdateRequestMessageParser(objReg)));
		regs.add(context.registerMessageParser(PCEPReportMessageParser.TYPE, new PCEPReportMessageParser(objReg)));
		regs.add(context.registerMessageSerializer(Pcrpt.class, new PCEPReportMessageParser(objReg)));
		regs.add(context.registerMessageParser(PCEPReplyMessageParser.TYPE, new PCEPReplyMessageParser(objReg)));
		regs.add(context.registerMessageSerializer(Pcrep.class, new PCEPReplyMessageParser(objReg)));
		regs.add(context.registerMessageParser(PCEPRequestMessageParser.TYPE, new PCEPRequestMessageParser(objReg)));
		regs.add(context.registerMessageSerializer(Pcreq.class, new PCEPRequestMessageParser(objReg)));

		final TlvHandlerRegistry tlvReg = context.getTlvHandlerRegistry();
		regs.add(context.registerObjectParser(PCEPLspObjectParser.CLASS, PCEPLspObjectParser.TYPE, new PCEPLspObjectParser(tlvReg)));
		regs.add(context.registerObjectSerializer(Lsp.class, new PCEPLspObjectParser(tlvReg)));
		regs.add(context.registerObjectParser(PCEPSrpObjectParser.CLASS, PCEPSrpObjectParser.TYPE, new PCEPSrpObjectParser(tlvReg)));
		regs.add(context.registerObjectSerializer(Srp.class, new PCEPSrpObjectParser(tlvReg)));
		regs.add(context.registerObjectParser(PCEPOpenObjectParser.CLASS, PCEPOpenObjectParser.TYPE, new PCEPOpenObjectParser(tlvReg)));
		regs.add(context.registerObjectSerializer(Open.class, new PCEPOpenObjectParser(tlvReg)));

		regs.add(context.registerTlvParser(LSPIdentifierIpv4TlvParser.TYPE, new LSPIdentifierIpv4TlvParser()));
		regs.add(context.registerTlvParser(LSPIdentifierIpv6TlvParser.TYPE, new LSPIdentifierIpv6TlvParser()));
		regs.add(context.registerTlvSerializer(LspIdentifiers.class, new LSPIdentifierIpv4TlvParser()));
		regs.add(context.registerTlvParser(LspUpdateErrorTlvParser.TYPE, new LspUpdateErrorTlvParser()));
		regs.add(context.registerTlvSerializer(LspErrorCode.class, new LspUpdateErrorTlvParser()));
		regs.add(context.registerTlvParser(RSVPErrorSpecTlvParser.TYPE, new RSVPErrorSpecTlvParser()));
		regs.add(context.registerTlvSerializer(RsvpErrorSpec.class, new RSVPErrorSpecTlvParser()));
		regs.add(context.registerTlvParser(PCEStatefulCapabilityTlvParser.TYPE, new PCEStatefulCapabilityTlvParser()));
		regs.add(context.registerTlvSerializer(Stateful.class, new PCEStatefulCapabilityTlvParser()));
		regs.add(context.registerTlvParser(LspSymbolicNameTlvParser.TYPE, new LspSymbolicNameTlvParser()));
		regs.add(context.registerTlvSerializer(SymbolicPathName.class, new LspSymbolicNameTlvParser()));

		return regs;
	}
}
