/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.initiated00;

import java.net.InetSocketAddress;

import org.opendaylight.protocol.pcep.spi.AbstractPCEPSessionProposalFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public class Stateful07SessionProposalFactory extends AbstractPCEPSessionProposalFactory {

	private final boolean stateful, active, instant;

	public Stateful07SessionProposalFactory(final int deadTimer, final int keepAlive, final boolean stateful, final boolean active,
			final boolean instant) {
		super(deadTimer, keepAlive);
		this.stateful = stateful;
		this.active = active;
		this.instant = instant;
	}

	@Override
	protected void addTlvs(final InetSocketAddress address, final TlvsBuilder builder) {
		if (Stateful07SessionProposalFactory.this.stateful) {
			builder.addAugmentation(
					Tlvs2.class,
					new Tlvs2Builder().setStateful(
							new StatefulBuilder().setLspUpdateCapability(this.active).addAugmentation(Stateful1.class,
									new Stateful1Builder().setInitiation(this.instant).build()).build()).build()).build();
		}
	}

	public boolean isStateful() {
		return this.stateful;
	}

	public boolean isActive() {
		return this.active;
	}

	@Deprecated
	public boolean isVersioned() {
		return false;
	}

	public boolean isInstant() {
		return this.instant;
	}

}
