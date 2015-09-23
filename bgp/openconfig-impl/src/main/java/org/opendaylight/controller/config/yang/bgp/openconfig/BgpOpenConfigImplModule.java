/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bgp.openconfig;

import org.opendaylight.protocol.bgp.openconfig.impl.BGPOpenConfig;

public class BgpOpenConfigImplModule extends org.opendaylight.controller.config.yang.bgp.openconfig.AbstractBgpOpenConfigImplModule {
    public BgpOpenConfigImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BgpOpenConfigImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.bgp.openconfig.BgpOpenConfigImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final BGPOpenConfig bgpOpenConfigProvider = new BGPOpenConfig();
        getBindingBrokerDependency().registerConsumer(bgpOpenConfigProvider);
        return bgpOpenConfigProvider;
    }

}
