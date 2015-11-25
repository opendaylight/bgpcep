/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bmp.impl;

import com.google.common.base.Optional;
import org.opendaylight.protocol.bmp.impl.BmpDispatcherImpl;
import org.opendaylight.protocol.bmp.impl.session.DefaultBmpSessionFactory;
import org.opendaylight.tcpmd5.netty.MD5ChannelFactory;
import org.opendaylight.tcpmd5.netty.MD5ServerChannelFactory;

public class BmpDispatcherImplModule extends org.opendaylight.controller.config.yang.bmp.impl.AbstractBmpDispatcherImplModule {
    public BmpDispatcherImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BmpDispatcherImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.bmp.impl.BmpDispatcherImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new BmpDispatcherImpl(getBossGroupDependency(), getWorkerGroupDependency(),
                getBmpExtensionsDependency().getBmpMessageRegistry(), new DefaultBmpSessionFactory(),
                Optional.<MD5ChannelFactory<?>>fromNullable(getMd5ChannelFactoryDependency()),
                Optional.<MD5ServerChannelFactory<?>>fromNullable(getMd5ServerChannelFactoryDependency()));
    }

}
