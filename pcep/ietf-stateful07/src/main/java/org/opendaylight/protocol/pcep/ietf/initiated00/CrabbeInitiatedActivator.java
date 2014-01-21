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
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.Stateful;

public final class CrabbeInitiatedActivator extends AbstractPCEPExtensionProviderActivator {
	@Override
	protected List<AutoCloseable> startImpl(final PCEPExtensionProviderContext context) {
		final List<AutoCloseable> regs = new ArrayList<>();

		regs.add(context.registerMessageParser(PcinitiateMessageParser.TYPE,
				new PcinitiateMessageParser(context.getObjectHandlerRegistry())));
		regs.add(context.registerMessageSerializer(Pcinitiate.class, new PcinitiateMessageParser(context.getObjectHandlerRegistry())));

		final TlvHandlerRegistry tlvReg = context.getTlvHandlerRegistry();
		regs.add(context.registerObjectParser(PCEPLspObjectParser.CLASS, PCEPLspObjectParser.TYPE, new PCEPLspObjectParser(tlvReg)));
		regs.add(context.registerObjectSerializer(Lsp.class, new PCEPLspObjectParser(tlvReg)));
		regs.add(context.registerObjectParser(PCEPSrpObjectParser.CLASS, PCEPSrpObjectParser.TYPE, new PCEPSrpObjectParser(tlvReg)));
		regs.add(context.registerObjectSerializer(Srp.class, new PCEPSrpObjectParser(tlvReg)));

		regs.add(context.registerTlvParser(PCEStatefulCapabilityTlvParser.TYPE, new PCEStatefulCapabilityTlvParser()));
		regs.add(context.registerTlvSerializer(Stateful.class, new PCEStatefulCapabilityTlvParser()));

		return regs;
	}
}
