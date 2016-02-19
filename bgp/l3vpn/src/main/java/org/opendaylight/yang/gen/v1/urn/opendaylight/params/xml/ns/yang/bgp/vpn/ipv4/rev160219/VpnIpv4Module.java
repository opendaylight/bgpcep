/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160219;

import org.opendaylight.protocol.bgp.l3vpn.BGPActivator;
import org.opendaylight.protocol.bgp.l3vpn.RIBActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;

public class VpnIpv4Module extends AbstractVpnIpv4Module {
    public VpnIpv4Module(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VpnIpv4Module(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev160219.VpnIpv4Module oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final class VpnIpv4Extension implements AutoCloseable, BGPExtensionProviderActivator, RIBExtensionProviderActivator {
            private final BGPExtensionProviderActivator bgpact = new BGPActivator();
            private final RIBExtensionProviderActivator ribact = new RIBActivator();
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
        return new VpnIpv4Extension();
    }

}
