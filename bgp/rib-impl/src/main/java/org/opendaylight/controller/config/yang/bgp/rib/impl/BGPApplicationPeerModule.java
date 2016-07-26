/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import org.opendaylight.protocol.bgp.rib.impl.BGPApplicationPeerClusterSingletonService;

/**
 * Application peer handler which handles translation from custom RIB into local RIB
 */
public class BGPApplicationPeerModule extends org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractBGPApplicationPeerModule {

    public BGPApplicationPeerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BGPApplicationPeerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.bgp.rib.impl.BGPApplicationPeerModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new BGPApplicationPeerClusterSingletonService(getApplicationRibId(), getDataBrokerDependency(), getTargetRibDependency(),
            getBgpPeerId(), getBgpPeerRegistryDependency(), getIdentifier().getInstanceName());
    }
}
