/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.pcep.stateful07.cfg;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.ietf.stateful07.PCEPStatefulCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public class PCEPStatefulCapabilityModule extends org.opendaylight.controller.config.yang.pcep.stateful07.cfg.AbstractPCEPStatefulCapabilityModule {

    public PCEPStatefulCapabilityModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public PCEPStatefulCapabilityModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final PCEPStatefulCapabilityModule oldModule, final java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation() {
        if ((getActive() || isSynchronizationAvoidance() || getIncludeDbVersion()) && !getStateful()) {
            setStateful(true);
        }
        if ((getTriggeredInitialSync() || getDeltaLspSyncCapability()) && !getIncludeDbVersion()) {
            setIncludeDbVersion(true);
        }
    }

    private boolean isSynchronizationAvoidance() {
        return getTriggeredInitialSync() || getTriggeredResync() || getDeltaLspSyncCapability();
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final PCEPStatefulCapability capabilityImpl = new PCEPStatefulCapability(getStateful(), getActive(), getInitiated(),
            getTriggeredInitialSync(), getTriggeredResync(), getDeltaLspSyncCapability(), getIncludeDbVersion());
        return new PCEPCapabilityImplCloseable(capabilityImpl);
    }

    private static final class PCEPCapabilityImplCloseable implements PCEPCapability, AutoCloseable {
        private final PCEPStatefulCapability innerCapabilityImpl;
        public PCEPCapabilityImplCloseable(final PCEPStatefulCapability capabilityImpl) {
            this.innerCapabilityImpl = Preconditions.checkNotNull(capabilityImpl);
        }
        @Override
        public void close() {
        }
        @Override
        public void setCapabilityProposal(final InetSocketAddress address, final TlvsBuilder builder) {
            this.innerCapabilityImpl.setCapabilityProposal(address, builder);
        }
    }
}
