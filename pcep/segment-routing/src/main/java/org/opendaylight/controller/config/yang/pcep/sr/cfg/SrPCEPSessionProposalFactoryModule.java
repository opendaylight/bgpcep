/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.pcep.sr.cfg;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.segment.routing.SegmentRoutingSessionProposalFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SrPCEPSessionProposalFactoryModule extends org.opendaylight.controller.config.yang.pcep.sr.cfg.AbstractSrPCEPSessionProposalFactoryModule {

    private static final String VALUE_IS_NOT_SET = "value is not set.";

    private static final int DEADTIMER_KEEPALIVE_RATIO = 4;

    private static final Logger LOG = LoggerFactory.getLogger(SrPCEPSessionProposalFactoryModule.class);

    public SrPCEPSessionProposalFactoryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SrPCEPSessionProposalFactoryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.pcep.sr.cfg.SrPCEPSessionProposalFactoryModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkNotNull(getActive(), VALUE_IS_NOT_SET, activeJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getInitiated(), VALUE_IS_NOT_SET, initiatedJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getDeadTimerValue(), VALUE_IS_NOT_SET, deadTimerValueJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getKeepAliveTimerValue(), VALUE_IS_NOT_SET, keepAliveTimerValueJmxAttribute);
        if (getKeepAliveTimerValue() != 0) {
            JmxAttributeValidationException.checkCondition(getKeepAliveTimerValue() >= 1, "minimum value is 1.",
                    keepAliveTimerValueJmxAttribute);
            if (getDeadTimerValue() != 0 && (getDeadTimerValue() / getKeepAliveTimerValue() != DEADTIMER_KEEPALIVE_RATIO)) {
                LOG.warn("DeadTimerValue should be 4 times greater than KeepAliveTimerValue");
            }
        }
        if (getActive() && !getStateful()) {
            setStateful(true);
        }
        JmxAttributeValidationException.checkNotNull(getStateful(), VALUE_IS_NOT_SET, statefulJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getSrCapable(), VALUE_IS_NOT_SET, srCapableJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final SegmentRoutingSessionProposalFactory inner = new SegmentRoutingSessionProposalFactory(getDeadTimerValue(), getKeepAliveTimerValue(), getStateful(), getActive(), getInitiated(), getSrCapable());
        return new PCEPSessionProposalFactoryCloseable(inner);
    }

    private static final class PCEPSessionProposalFactoryCloseable implements PCEPSessionProposalFactory, AutoCloseable {
        private final SegmentRoutingSessionProposalFactory inner;

        public PCEPSessionProposalFactoryCloseable(final SegmentRoutingSessionProposalFactory inner) {
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
