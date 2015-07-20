package org.opendaylight.controller.config.yang.bgp.openconfig;

import org.opendaylight.bgpcep.bgp.openconfig.BgpConfigWriter;

public class BgpOpenonfigImplModule extends org.opendaylight.controller.config.yang.bgp.openconfig.AbstractBgpOpenonfigImplModule {
    public BgpOpenonfigImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BgpOpenonfigImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.bgp.openconfig.BgpOpenonfigImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final BgpConfigWriter configWriter = new BgpConfigWriter();
        getBindingBrokerDependency().registerConsumer(configWriter);
        return configWriter;
    }

}
