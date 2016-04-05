/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.evpn;

import org.opendaylight.protocol.bgp.evpn.impl.BGPActivator;
import org.opendaylight.protocol.bgp.evpn.impl.RIBActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;

public class EvpnModule extends org.opendaylight.controller.config.yang.bgp.evpn.AbstractEvpnModule {
    public EvpnModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public EvpnModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.bgp.evpn.EvpnModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final class EvpnExtension implements AutoCloseable, BGPExtensionProviderActivator, RIBExtensionProviderActivator {
            private final BGPExtensionProviderActivator bgpActivator = new BGPActivator();
            private final RIBExtensionProviderActivator ribActivator = new RIBActivator();

            @Override
            public void close() throws Exception {
                if (this.bgpActivator != null) {
                    this.bgpActivator.stop();
                }
                if (this.ribActivator != null) {
                    this.ribActivator.stopRIBExtensionProvider();
                }
            }

            @Override
            public void start(final BGPExtensionProviderContext context) {
                this.bgpActivator.start(context);
            }

            @Override
            public void stop() {
                this.bgpActivator.stop();
            }

            @Override
            public void startRIBExtensionProvider(final RIBExtensionProviderContext context) {
                this.ribActivator.startRIBExtensionProvider(context);
            }

            @Override
            public void stopRIBExtensionProvider() {
                this.ribActivator.stopRIBExtensionProvider();
            }
        }
        return new EvpnExtension();
    }

}
