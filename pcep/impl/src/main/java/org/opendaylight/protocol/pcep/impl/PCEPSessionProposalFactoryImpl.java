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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.StatefulBuilder;

public class PCEPSessionProposalFactoryImpl implements PCEPSessionProposalFactory {

	private final int keepAlive, deadTimer;

	private final boolean stateful, active, versioned, instant;

	public PCEPSessionProposalFactoryImpl(final int deadTimer, final int keepAlive, final boolean stateful, final boolean active,
			final boolean versioned, final boolean instant) {
		this.deadTimer = deadTimer;
		this.keepAlive = keepAlive;
		this.stateful = stateful;
		this.active = active;
		this.versioned = versioned;
		this.instant = instant;
	}

	@Override
	public Open getSessionProposal(final InetSocketAddress address, final int sessionId) {
		final TlvsBuilder builder = new TlvsBuilder();
		if (PCEPSessionProposalFactoryImpl.this.stateful) {
			builder.setStateful((new StatefulBuilder().setIncludeDbVersion(this.versioned).setLspUpdateCapability(this.active).addAugmentation(
					Stateful1.class, new Stateful1Builder().setInitiation(this.instant).build()).build()));
		}
		final OpenBuilder oBuilder = new OpenBuilder();
		oBuilder.setSessionId((short) sessionId);
		if (PCEPSessionProposalFactoryImpl.this.keepAlive != 0) {
			oBuilder.setKeepalive((short) PCEPSessionProposalFactoryImpl.this.keepAlive);
		}
		if (PCEPSessionProposalFactoryImpl.this.deadTimer != 0) {
			oBuilder.setDeadTimer((short) PCEPSessionProposalFactoryImpl.this.deadTimer);
		}
		return oBuilder.setTlvs(builder.build()).build();
	}

	public int getKeepAlive() {
		return this.keepAlive;
	}

	public int getDeadTimer() {
		return this.deadTimer;
	}

	public boolean isStateful() {
		return this.stateful;
	}

	public boolean isActive() {
		return this.active;
	}

	public boolean isVersioned() {
		return this.versioned;
	}

	public boolean isInstant() {
		return this.instant;
	}

}
