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
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.segment.routing.PCEPSegmentRoutingCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public class PCEPSegmentRoutingCapabilityModule extends org.opendaylight.controller.config.yang.pcep.sr.cfg.AbstractPCEPSegmentRoutingCapabilityModule {

    public PCEPSegmentRoutingCapabilityModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public PCEPSegmentRoutingCapabilityModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.pcep.sr.cfg.PCEPSegmentRoutingCapabilityModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final PCEPSegmentRoutingCapability inner = new PCEPSegmentRoutingCapability(getSrCapable());
        return new PCEPSegmentRoutingCapabilityCloseable(inner);
    }

    private static final class PCEPSegmentRoutingCapabilityCloseable implements PCEPCapability, AutoCloseable {
        private final PCEPSegmentRoutingCapability inner;

        public PCEPSegmentRoutingCapabilityCloseable(final PCEPSegmentRoutingCapability inner) {
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
