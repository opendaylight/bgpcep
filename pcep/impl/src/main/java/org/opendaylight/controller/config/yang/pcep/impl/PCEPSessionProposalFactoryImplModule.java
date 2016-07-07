/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * Generated file

 * Generated from: yang module name: pcep-impl  yang module local name: pcep-session-proposal-factory-impl
 * Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
 * Generated at: Wed Nov 06 13:16:39 CET 2013
 *
 * Do not modify this file unless it is present under src/main directory
 */
package org.opendaylight.controller.config.yang.pcep.impl;

import java.net.InetSocketAddress;
import java.util.List;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPPeerProposal;
import org.opendaylight.protocol.pcep.PCEPSessionProposalFactory;
import org.opendaylight.protocol.pcep.impl.BasePCEPSessionProposalFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public final class PCEPSessionProposalFactoryImplModule extends
        org.opendaylight.controller.config.yang.pcep.impl.AbstractPCEPSessionProposalFactoryImplModule {

    private static final Logger LOG = LoggerFactory.getLogger(PCEPSessionProposalFactoryImplModule.class);

    private static final int KA_TO_DEADTIMER_RATIO = 4;

    public PCEPSessionProposalFactoryImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier name,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(name, dependencyResolver);
    }

    public PCEPSessionProposalFactoryImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier name,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final PCEPSessionProposalFactoryImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(name, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkNotNull(getDeadTimerValue(), "value is not set.", deadTimerValueJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getKeepAliveTimerValue(), "value is not set.", keepAliveTimerValueJmxAttribute);
        if (getKeepAliveTimerValue() != 0) {
            JmxAttributeValidationException.checkCondition(getKeepAliveTimerValue() >= 1, "minimum value is 1.",
                    keepAliveTimerValueJmxAttribute);
            if (getDeadTimerValue() != 0 && (getDeadTimerValue() / getKeepAliveTimerValue() != KA_TO_DEADTIMER_RATIO)) {
                LOG.warn("DeadTimerValue should be 4 times greater than KeepAliveTimerValue");
            }
        }
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final BasePCEPSessionProposalFactory inner = new BasePCEPSessionProposalFactory(getDeadTimerValue(), getKeepAliveTimerValue(), getCapabilityDependency());
        return new PCEPSessionProposalFactoryCloseable(inner);
    }

    private static final class PCEPSessionProposalFactoryCloseable implements PCEPSessionProposalFactory, AutoCloseable {

        private final BasePCEPSessionProposalFactory inner;

        public PCEPSessionProposalFactoryCloseable(final BasePCEPSessionProposalFactory inner) {
            this.inner = inner;
        }

        @Override
        public void close() {
            // Nothing to do
        }

        @Override
        public Open getSessionProposal(final InetSocketAddress inetSocketAddress, final int i) {
            return this.inner.getSessionProposal(inetSocketAddress, i);
        }

        @Override
        public Open getSessionProposal(final InetSocketAddress address,
                final int sessionId, final PCEPPeerProposal peerProposal) {
            return this.inner.getSessionProposal(address, sessionId, peerProposal);
        }

        @Override
        public List<PCEPCapability> getCapabilities() {
            return this.inner.getCapabilities();
        }
    }
}
