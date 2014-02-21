/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.pcep.stateful02.cfg;

import java.net.InetSocketAddress;

import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02SessionProposalFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 *
 */
public final class Stateful02PCEPSessionProposalFactoryModule extends org.opendaylight.controller.config.yang.pcep.stateful02.cfg.AbstractStateful02PCEPSessionProposalFactoryModule
{
	private static final Logger LOG = LoggerFactory.getLogger(Stateful02PCEPSessionProposalFactoryModule.class);

	public Stateful02PCEPSessionProposalFactoryModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
		super(identifier, dependencyResolver);
	}

	public Stateful02PCEPSessionProposalFactoryModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
			final Stateful02PCEPSessionProposalFactoryModule oldModule, final java.lang.AutoCloseable oldInstance) {

		super(identifier, dependencyResolver, oldModule, oldInstance);
	}

	@Override
	protected void customValidation(){
		JmxAttributeValidationException.checkNotNull(getActive(), "value is not set.", this.activeJmxAttribute);
		JmxAttributeValidationException.checkNotNull(getInitiated(), "value is not set.", this.initiatedJmxAttribute);
		JmxAttributeValidationException.checkNotNull(getDeadTimerValue(), "value is not set.", this.deadTimerValueJmxAttribute);
		JmxAttributeValidationException.checkNotNull(getKeepAliveTimerValue(), "value is not set.", this.keepAliveTimerValueJmxAttribute);
		if (getKeepAliveTimerValue() != 0) {
			JmxAttributeValidationException.checkCondition(getKeepAliveTimerValue() >= 1, "minimum value is 1.",
					this.keepAliveTimerValueJmxAttribute);
			if (getDeadTimerValue() != 0 && (getDeadTimerValue() / getKeepAliveTimerValue() != 4)) {
				LOG.warn("DeadTimerValue should be 4 times greater than KeepAliveTimerValue");
			}
		}
		if (getActive() && !getStateful()) {
			setStateful(true);
		}
		JmxAttributeValidationException.checkNotNull(getStateful(), "value is not set.", this.statefulJmxAttribute);
	}

	@Override
	public java.lang.AutoCloseable createInstance() {
		final Stateful02SessionProposalFactory inner = new Stateful02SessionProposalFactory(getDeadTimerValue(), getKeepAliveTimerValue(), getStateful(), getActive(), getInitiated());
		return new PCEPSessionProposalFactoryCloseable(inner);
	}

	private static final class PCEPSessionProposalFactoryCloseable implements PCEPSessionProposalFactory, AutoCloseable {
		private final Stateful02SessionProposalFactory inner;

		public PCEPSessionProposalFactoryCloseable(final Stateful02SessionProposalFactory inner) {
			this.inner = Preconditions.checkNotNull(inner);
		}

		@Override
		public void close() {
		}

		@Override
		public Open getSessionProposal(final InetSocketAddress inetSocketAddress, final int sessionId) {
			return this.inner.getSessionProposal(inetSocketAddress, sessionId);
		}
	}
}
