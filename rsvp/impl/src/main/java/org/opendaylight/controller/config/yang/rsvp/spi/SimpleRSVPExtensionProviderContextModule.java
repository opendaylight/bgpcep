package org.opendaylight.controller.config.yang.rsvp.spi;
public class SimpleRSVPExtensionProviderContextModule extends org.opendaylight.controller.config.yang.rsvp.spi.AbstractSimpleRSVPExtensionProviderContextModule {
    public SimpleRSVPExtensionProviderContextModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SimpleRSVPExtensionProviderContextModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.rsvp.spi.SimpleRSVPExtensionProviderContextModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // TODO:implement
        throw new java.lang.UnsupportedOperationException();
    }

}
