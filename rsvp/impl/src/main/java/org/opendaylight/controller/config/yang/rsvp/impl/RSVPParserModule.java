package org.opendaylight.controller.config.yang.rsvp.impl;

import org.opendaylight.protocol.rsvp.parser.impl.RSVPActivator;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionProviderActivator;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionProviderContext;

public class RSVPParserModule extends org.opendaylight.controller.config.yang.rsvp.impl.AbstractRSVPParserModule {
    public RSVPParserModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public RSVPParserModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.rsvp.impl.RSVPParserModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final class RSVPExtension implements AutoCloseable, RSVPExtensionProviderActivator {
            private final RSVPExtensionProviderActivator activator = new RSVPActivator();

            @Override
            public void close() {
                if (activator != null) {
                    activator.stop();
                }
            }

            @Override
            public void start(final RSVPExtensionProviderContext context) {
                activator.start(context);
            }

            @Override
            public void stop() {
                activator.stop();
            }
        }

        return new RSVPExtension();
    }

}
