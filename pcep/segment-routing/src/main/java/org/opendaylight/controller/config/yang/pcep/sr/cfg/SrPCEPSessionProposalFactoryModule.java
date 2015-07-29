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
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.segment.routing.SegmentRoutingSessionProposalFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SrPCEPSessionProposalFactoryModule extends org.opendaylight.controller.config.yang.pcep.sr.cfg.AbstractSrPCEPSessionProposalFactoryModule {

    private static final String VALUE_IS_NOT_SET = "value is not set.";

    private static final int DEADTIMER_KEEPALIVE_RATIO = 4;

    private static final Logger LOG = LoggerFactory.getLogger(SrPCEPSessionProposalFactoryModule.class);

    public SrPCEPSessionProposalFactoryModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SrPCEPSessionProposalFactoryModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.pcep.sr.cfg.SrPCEPSessionProposalFactoryModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkNotNull(getActive(), VALUE_IS_NOT_SET, activeJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getInitiated(), VALUE_IS_NOT_SET, initiatedJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getDeadTimerValue(), VALUE_IS_NOT_SET, deadTimerValueJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getKeepAliveTimerValue(), VALUE_IS_NOT_SET, keepAliveTimerValueJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getTriggeredInitialSync(), VALUE_IS_NOT_SET, triggeredInitialSyncJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getTriggeredResync(), VALUE_IS_NOT_SET, triggeredResyncJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getDeltaLspSyncCapability(), VALUE_IS_NOT_SET, deltaLspSyncCapabilityJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getIncludeDbVersion(), VALUE_IS_NOT_SET, includeDbVersionJmxAttribute);
        validateTimers();
        if ((getActive() || getTriggeredInitialSync() || getTriggeredResync() || getDeltaLspSyncCapability() || getIncludeDbVersion()) && !getStateful()) {
            setStateful(true);
        }
        if ((getTriggeredInitialSync() || getDeltaLspSyncCapability()) && !getIncludeDbVersion()) {
            setIncludeDbVersion(true);
        }
        JmxAttributeValidationException.checkNotNull(getStateful(), VALUE_IS_NOT_SET, statefulJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getSrCapable(), VALUE_IS_NOT_SET, srCapableJmxAttribute);
    }

    private void validateTimers() {
        if (getKeepAliveTimerValue() != 0) {
            JmxAttributeValidationException.checkCondition(getKeepAliveTimerValue() >= 1, "minimum value is 1.", keepAliveTimerValueJmxAttribute);
            if (getDeadTimerValue() != 0 && (getDeadTimerValue() / getKeepAliveTimerValue() != DEADTIMER_KEEPALIVE_RATIO)) {
                LOG.warn("DeadTimerValue should be 4 times greater than KeepAliveTimerValue");
            }
        }
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final SegmentRoutingSessionProposalFactory inner = new SegmentRoutingSessionProposalFactory(getStateful(), getActive(), getInitiated(), getSrCapable(),
            getTriggeredInitialSync(), getTriggeredResync(), getDeltaLspSyncCapability(), getIncludeDbVersion());
        return new PCEPSessionProposalFactoryCloseable(inner);
    }

    private static final class PCEPSessionProposalFactoryCloseable implements PCEPCapability, AutoCloseable {
        private final SegmentRoutingSessionProposalFactory inner;

        public PCEPSessionProposalFactoryCloseable(final SegmentRoutingSessionProposalFactory inner) {
            this.inner = Preconditions.checkNotNull(inner);
        }

        @Override
        public void close() {
        }

        @Override
        public void setCapabilityProposal(final InetSocketAddress address, final TlvsBuilder builder) {
            this.inner.setCapabilityProposal(address, builder);
        }

    }

}
