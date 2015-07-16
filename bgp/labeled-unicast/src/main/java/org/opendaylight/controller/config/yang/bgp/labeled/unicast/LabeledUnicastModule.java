/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.labeled.unicast;

import org.opendaylight.protocol.bgp.labeled.unicast.BGPActivator;
import org.opendaylight.protocol.bgp.labeled.unicast.RIBActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;

public class LabeledUnicastModule extends org.opendaylight.controller.config.yang.bgp.labeled.unicast.AbstractLabeledUnicastModule {
    public LabeledUnicastModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public LabeledUnicastModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.bgp.labeled.unicast.LabeledUnicastModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final class LabeledUnicastExtension implements AutoCloseable, BGPExtensionProviderActivator, RIBExtensionProviderActivator {
            private final BGPExtensionProviderActivator bgpact = new BGPActivator();
            private final RIBExtensionProviderActivator ribact = new RIBActivator();
            @Override
            public void startRIBExtensionProvider(
                    RIBExtensionProviderContext context) {
                this.ribact.startRIBExtensionProvider(context);
            }
            @Override
            public void stopRIBExtensionProvider() {
                this.ribact.stopRIBExtensionProvider();
            }
            @Override
            public void start(BGPExtensionProviderContext context) {
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
        return new LabeledUnicastExtension();
    }

}
