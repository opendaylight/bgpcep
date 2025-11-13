/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2021 PANTHEON.tech, s.r.o.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.pojo;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.ServiceLoader;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.LabelRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionConsumerContext;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionProviderActivator;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionProviderContext;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * Starts and stops {@link RSVPExtensionProviderActivator} instances for an {@link RSVPExtensionProviderContext}.
 */
@Singleton
@Component(immediate = true)
@MetaInfServices
public final class DefaultRSVPExtensionConsumerContext implements RSVPExtensionConsumerContext {
    private final @NonNull SimpleRSVPExtensionProviderContext delegate = new SimpleRSVPExtensionProviderContext();

    public DefaultRSVPExtensionConsumerContext() {
        this(ImmutableList.copyOf(ServiceLoader.load(RSVPExtensionProviderActivator.class)));
    }

    @Inject
    @Activate
    public DefaultRSVPExtensionConsumerContext(
            @Reference(policyOption = ReferencePolicyOption.GREEDY)
            final List<RSVPExtensionProviderActivator> extensionActivators) {
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
