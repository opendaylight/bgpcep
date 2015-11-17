/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.flowspec;

import org.opendaylight.protocol.bgp.flowspec.BGPActivator;
import org.opendaylight.protocol.bgp.flowspec.RIBActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;

public class FlowspecModule extends org.opendaylight.controller.config.yang.bgp.flowspec.AbstractFlowspecModule {
    public FlowspecModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public FlowspecModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.bgp.flowspec.FlowspecModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final class FlowspecExtension implements AutoCloseable, BGPExtensionProviderActivator, RIBExtensionProviderActivator {
            private final BGPActivator bgpact = new BGPActivator();
            private final RIBExtensionProviderActivator ribact = new RIBActivator();

            @Override
            public void close() {
                if (this.bgpact != null) {
                    this.bgpact.stop();
                }
                if (this.ribact != null) {
                    this.ribact.stopRIBExtensionProvider();
                }
            }

            @Override
            public void startRIBExtensionProvider(final RIBExtensionProviderContext context) {
                this.ribact.startRIBExtensionProvider(context);
            }

            @Override
            public void stopRIBExtensionProvider() {
                this.ribact.stopRIBExtensionProvider();
            }

            @Override
            public void start(final BGPExtensionProviderContext context) {
                this.bgpact.start(context);
            }

            @Override
            public void stop() {
                this.bgpact.stop();
                this.bgpact.stopFlowspecActivator();
            }
        }
        return new FlowspecExtension();
    }
}
