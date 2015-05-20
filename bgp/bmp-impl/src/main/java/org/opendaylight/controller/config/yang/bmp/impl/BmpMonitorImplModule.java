package org.opendaylight.controller.config.yang.bmp.impl;

import java.net.InetSocketAddress;

import org.opendaylight.protocol.bmp.impl.BmpMonitorImpl;
import org.opendaylight.tcpmd5.api.KeyMapping;

public class BmpMonitorImplModule extends org.opendaylight.controller.config.yang.bmp.impl.AbstractBmpMonitorImplModule {
    public BmpMonitorImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BmpMonitorImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.bmp.impl.BmpMonitorImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // TODO
        final KeyMapping keys = null;
        InetSocketAddress inetAddress = null;

        return BmpMonitorImpl.createBmpMonitorInstance(getExtensionsDependency(), getBmpDispatcherDependency()
            , getTcpReconnectStrategyDependency(), getSessionReconnectStrategyDependency(),
            getDomDataProviderDependency(), getMonitorId(), inetAddress, keys.isEmpty() ? null : keys);
    }
}
