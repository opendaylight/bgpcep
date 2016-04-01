/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.vpn.ipv6;

import org.opendaylight.protocol.bgp.l3vpn.ipv6.BgpIpv6Activator;
import org.opendaylight.protocol.bgp.l3vpn.ipv6.RibIpv6Activator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;

public class VpnIpv6Module extends org.opendaylight.controller.config.yang.bgp.vpn.ipv6.AbstractVpnIpv6Module {
    public VpnIpv6Module(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VpnIpv6Module(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.bgp.vpn.ipv6.VpnIpv6Module oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final class VpnIpv6Extension implements AutoCloseable, BGPExtensionProviderActivator, RIBExtensionProviderActivator {
            private final BGPExtensionProviderActivator bgpact = new BgpIpv6Activator();
            private final RIBExtensionProviderActivator ribact = new RibIpv6Activator();

            @Override
            public void startRIBExtensionProvider(
                final RIBExtensionProviderContext context) {
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
            }

            @Override
            public void close() throws Exception {
                if (this.bgpact != null) {
                    this.bgpact.stop();
                }
                if (this.ribact != null) {
                    this.ribact.stopRIBExtensionProvider();
                }
            }
        }
        return new VpnIpv6Extension();
    }

}
