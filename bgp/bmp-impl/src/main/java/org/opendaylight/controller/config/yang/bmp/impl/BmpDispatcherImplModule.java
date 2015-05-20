/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bmp.impl;

import org.opendaylight.protocol.bmp.api.BmpSessionListener;
import org.opendaylight.protocol.bmp.impl.BmpDispatcherImpl;
import org.opendaylight.protocol.bmp.spi.registry.BmpExtensionConsumerContext;
import org.opendaylight.protocol.framework.SessionListenerFactory;

public class BmpDispatcherImplModule extends org.opendaylight.controller.config.yang.bmp.impl.AbstractBmpDispatcherImplModule {
    public BmpDispatcherImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BmpDispatcherImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.bmp.impl.BmpDispatcherImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        //nothing to validate
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final BmpExtensionConsumerContext bmpExtensions = getBmpExtensionsDependency();

        final SessionListenerFactory<BmpSessionListener> sessionListenerFactory = null;

        return new BmpDispatcherImpl(bmpExtensions.getBmpMessageRegistry(),
            sessionListenerFactory,
            getBossGroupDependency(),
            getWorkerGroupDependency(),
            getMd5ChannelFactoryDependency(),
            getMd5ServerChannelFactoryDependency());
    }

}
