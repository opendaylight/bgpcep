/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPSessionPreferences;
import org.opendaylight.protocol.pcep.PCEPSessionProposal;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.PCEPTlv;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.protocol.pcep.tlv.LSPCleanupTlv;
import org.opendaylight.protocol.pcep.tlv.PCEStatefulCapabilityTlv;

public class PCEPSessionProposalFactoryImpl extends PCEPSessionProposalFactory implements Closeable {

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
	public PCEPSessionProposal getSessionProposal(final InetSocketAddress address, final int sessionId) {
		return new PCEPSessionProposal() {

			@Override
			public PCEPSessionPreferences getProposal() {
				List<PCEPTlv> tlvs = null;
				if (PCEPSessionProposalFactoryImpl.this.stateful) {
					tlvs = new ArrayList<PCEPTlv>();
					tlvs.add(new PCEStatefulCapabilityTlv(PCEPSessionProposalFactoryImpl.this.instant, PCEPSessionProposalFactoryImpl.this.active, PCEPSessionProposalFactoryImpl.this.versioned));
					if (PCEPSessionProposalFactoryImpl.this.instant) {
						tlvs.add(new LSPCleanupTlv(PCEPSessionProposalFactoryImpl.this.timeout));
					}
				}
				return new PCEPSessionPreferences(new PCEPOpenObject(PCEPSessionProposalFactoryImpl.this.keepAlive, PCEPSessionProposalFactoryImpl.this.deadTimer, sessionId, tlvs));
			}

		};
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

	@Override
	public void close() throws IOException {
		// nothing to close
	}
}
