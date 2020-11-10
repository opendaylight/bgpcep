/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.pojo;

import java.util.ServiceLoader;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.LabelRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionConsumerContext;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionProviderActivator;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectRegistry;
import org.osgi.service.component.annotations.Component;

/**
 * Starts and stops RSVPExtensionProviderActivator instances for a RSVPExtensionProviderContext.
 *
 * @author Thomas Pantelis
 */
@Singleton
@Component(immediate = true)
@MetaInfServices
public class DefaultRSVPExtensionConsumerContext implements RSVPExtensionConsumerContext {
    private final SimpleRSVPExtensionProviderContext delegate = new SimpleRSVPExtensionProviderContext();

    public DefaultRSVPExtensionConsumerContext() {
        this(ServiceLoader.load(RSVPExtensionProviderActivator.class));
    }

    @Inject
    public DefaultRSVPExtensionConsumerContext(final Iterable<RSVPExtensionProviderActivator> extensionActivators) {
        extensionActivators.forEach(activator -> activator.start(delegate));
    }

    @Override
    public RSVPTeObjectRegistry getRsvpRegistry() {
        return delegate.getRsvpRegistry();
    }

    @Override
    public XROSubobjectRegistry getXROSubobjectHandlerRegistry() {
        return delegate.getXROSubobjectHandlerRegistry();
    }

    @Override
    public EROSubobjectRegistry getEROSubobjectHandlerRegistry() {
        return delegate.getEROSubobjectHandlerRegistry();
    }

    @Override
    public RROSubobjectRegistry getRROSubobjectHandlerRegistry() {
        return delegate.getRROSubobjectHandlerRegistry();
    }

    @Override
    public LabelRegistry getLabelHandlerRegistry() {
        return delegate.getLabelHandlerRegistry();
    }
}
