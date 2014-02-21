/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.net.InetSocketAddress;

import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public class BasePCEPSessionProposalFactory implements PCEPSessionProposalFactory {

	private final int keepAlive, deadTimer;

	public BasePCEPSessionProposalFactory(final int deadTimer, final int keepAlive) {
		this.deadTimer = deadTimer;
		this.keepAlive = keepAlive;
	}

	protected void addTlvs(final InetSocketAddress address, final TlvsBuilder builder) {
		// No TLVs by default
	}

	@Override
	public final Open getSessionProposal(final InetSocketAddress address, final int sessionId) {
		final OpenBuilder oBuilder = new OpenBuilder();
		oBuilder.setSessionId((short) sessionId);
		if (this.keepAlive != 0) {
			oBuilder.setKeepalive((short) BasePCEPSessionProposalFactory.this.keepAlive);
		}
		if (this.deadTimer != 0) {
			oBuilder.setDeadTimer((short) BasePCEPSessionProposalFactory.this.deadTimer);
		}

		final TlvsBuilder builder = new TlvsBuilder();
		addTlvs(address, builder);
		return oBuilder.setTlvs(builder.build()).build();
	}

	public final int getKeepAlive() {
		return this.keepAlive;
	}

	public final int getDeadTimer() {
		return this.deadTimer;
	}
}
