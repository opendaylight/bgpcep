/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.pcep.stateful02.cfg;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02SessionProposalFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
@Deprecated
public final class Stateful02PCEPSessionProposalFactoryModule extends org.opendaylight.controller.config.yang.pcep.stateful02.cfg.AbstractStateful02PCEPSessionProposalFactoryModule {
    private static final Logger LOG = LoggerFactory.getLogger(Stateful02PCEPSessionProposalFactoryModule.class);

    private static final String NOT_SET = "value is not set.";
    private static final int RATIO = 4;

    public Stateful02PCEPSessionProposalFactoryModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
        final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public Stateful02PCEPSessionProposalFactoryModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
        final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
        final Stateful02PCEPSessionProposalFactoryModule oldModule, final java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation() {
        JmxAttributeValidationException.checkNotNull(getActive(), NOT_SET, activeJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getInitiated(), NOT_SET, initiatedJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getDeadTimerValue(), NOT_SET, deadTimerValueJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getKeepAliveTimerValue(), NOT_SET, keepAliveTimerValueJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getTimeout(), NOT_SET, timeoutJmxAttribute);
        if (getKeepAliveTimerValue() != 0) {
            JmxAttributeValidationException.checkCondition(getKeepAliveTimerValue() >= 1, "minimum value is 1.",
                keepAliveTimerValueJmxAttribute);
            if (getDeadTimerValue() != 0 && (getDeadTimerValue() / getKeepAliveTimerValue() != RATIO)) {
                LOG.warn("DeadTimerValue should be 4 times greater than KeepAliveTimerValue");
            }
        }
        if (getActive() && !getStateful()) {
            setStateful(true);
        }
        JmxAttributeValidationException.checkNotNull(getStateful(), "value is not set.", statefulJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final Stateful02SessionProposalFactory inner = new Stateful02SessionProposalFactory(getDeadTimerValue(), getKeepAliveTimerValue(), getStateful(), getActive(), getInitiated(), getTimeout());
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
