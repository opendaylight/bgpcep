/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.pcep.stateful07.cfg;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.ietf.initiated00.Stateful07SessionProposalFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public final class Stateful07PCEPSessionProposalFactoryModule extends
        org.opendaylight.controller.config.yang.pcep.stateful07.cfg.AbstractStateful07PCEPSessionProposalFactoryModule {
    private static final Logger LOG = LoggerFactory.getLogger(Stateful07PCEPSessionProposalFactoryModule.class);

    private static final String VALUE_IS_NOT_SET = "value is not set.";

    private static final int DT_KA_RATIO = 4;

    public Stateful07PCEPSessionProposalFactoryModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public Stateful07PCEPSessionProposalFactoryModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final Stateful07PCEPSessionProposalFactoryModule oldModule, final java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation() {
        JmxAttributeValidationException.checkNotNull(getActive(), VALUE_IS_NOT_SET, activeJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getInitiated(), VALUE_IS_NOT_SET, initiatedJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getDeadTimerValue(), VALUE_IS_NOT_SET, deadTimerValueJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getKeepAliveTimerValue(), VALUE_IS_NOT_SET, keepAliveTimerValueJmxAttribute);
        if (getKeepAliveTimerValue() != 0) {
            JmxAttributeValidationException.checkCondition(getKeepAliveTimerValue() >= 1, "minimum value is 1.",
                    keepAliveTimerValueJmxAttribute);
            if (getDeadTimerValue() != 0 && (getDeadTimerValue() / getKeepAliveTimerValue() != DT_KA_RATIO)) {
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
        final Stateful07SessionProposalFactory inner = new Stateful07SessionProposalFactory(getDeadTimerValue(), getKeepAliveTimerValue(), getStateful(), getActive(), getInitiated());
        return new PCEPSessionProposalFactoryCloseable(inner);
    }

    private static final class PCEPSessionProposalFactoryCloseable implements PCEPSessionProposalFactory, AutoCloseable {
        private final Stateful07SessionProposalFactory inner;

        public PCEPSessionProposalFactoryCloseable(final Stateful07SessionProposalFactory inner) {
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
