/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.net.InetSocketAddress;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.StatefulCapabilityTlv.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.tlvs.StatefulBuilder;

import com.google.common.collect.Lists;

public class PCEPSessionProposalFactoryImpl implements PCEPSessionProposalFactory {

	private final int keepAlive, deadTimer, timeout;

	private final boolean stateful, active, versioned, instant;

	public PCEPSessionProposalFactoryImpl(final int deadTimer, final int keepAlive, final boolean stateful, final boolean active, final boolean versioned, final boolean instant, final int timeout) {
		this.deadTimer = deadTimer;
		this.keepAlive = keepAlive;
		this.stateful = stateful;
		this.active = active;
		this.versioned = versioned;
		this.instant = instant;
		this.timeout = timeout;
	}

	@Override
	public PCEPOpenObject getSessionProposal(final InetSocketAddress address, final int sessionId) {
		List<Tlv> tlvs = null;
		if (PCEPSessionProposalFactoryImpl.this.stateful) {
			tlvs = Lists.newArrayList();
			tlvs.add(new StatefulBuilder().setFlags(new Flags(PCEPSessionProposalFactoryImpl.this.versioned, PCEPSessionProposalFactoryImpl.this.instant, PCEPSessionProposalFactoryImpl.this.active)).build());
		}
		return new PCEPOpenObject(PCEPSessionProposalFactoryImpl.this.keepAlive, PCEPSessionProposalFactoryImpl.this.deadTimer, sessionId, tlvs);
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

	public int getTimeout() {
		return this.timeout;
	}
}
