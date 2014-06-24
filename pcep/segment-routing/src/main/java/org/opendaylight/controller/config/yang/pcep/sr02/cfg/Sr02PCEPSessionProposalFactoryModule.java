/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.pcep.sr02.cfg;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.ietf.initiated00.Stateful07SessionProposalFactory;
import org.opendaylight.protocol.pcep.segment.routing02.SegmentRouting02SessionProposalFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sr02PCEPSessionProposalFactoryModule extends org.opendaylight.controller.config.yang.pcep.sr02.cfg.AbstractSr02PCEPSessionProposalFactoryModule {
    private static final Logger LOG = LoggerFactory.getLogger(Sr02PCEPSessionProposalFactoryModule.class);

    public Sr02PCEPSessionProposalFactoryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public Sr02PCEPSessionProposalFactoryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.pcep.sr02.cfg.Sr02PCEPSessionProposalFactoryModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkNotNull(getActive(), "value is not set.", activeJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getInitiated(), "value is not set.", initiatedJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getDeadTimerValue(), "value is not set.", deadTimerValueJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getKeepAliveTimerValue(), "value is not set.", keepAliveTimerValueJmxAttribute);
        if (getKeepAliveTimerValue() != 0) {
            JmxAttributeValidationException.checkCondition(getKeepAliveTimerValue() >= 1, "minimum value is 1.",
                    keepAliveTimerValueJmxAttribute);
            if (getDeadTimerValue() != 0 && (getDeadTimerValue() / getKeepAliveTimerValue() != 4)) {
                LOG.warn("DeadTimerValue should be 4 times greater than KeepAliveTimerValue");
            }
        }
        if (getActive() && !getStateful()) {
            setStateful(true);
        }
        JmxAttributeValidationException.checkNotNull(getStateful(), "value is not set.", statefulJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getSrCapable(), "value is not set.", srCapableJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final SegmentRouting02SessionProposalFactory inner = new SegmentRouting02SessionProposalFactory(getDeadTimerValue(), getKeepAliveTimerValue(), getStateful(), getActive(), getInitiated(), getSrCapable());
        return new PCEPSessionProposalFactoryCloseable(inner);
    }

    private static final class PCEPSessionProposalFactoryCloseable implements PCEPSessionProposalFactory, AutoCloseable {
        private final Stateful07SessionProposalFactory inner;

        public PCEPSessionProposalFactoryCloseable(final SegmentRouting02SessionProposalFactory inner) {
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
