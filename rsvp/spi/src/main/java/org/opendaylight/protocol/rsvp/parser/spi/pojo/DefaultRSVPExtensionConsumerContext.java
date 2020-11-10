/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.pojo;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.ServiceLoader;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionConsumerContext;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionProviderActivator;

/**
 * Starts and stops RSVPExtensionProviderActivator instances for a RSVPExtensionProviderContext.
 *
 * @author Thomas Pantelis
 */
@Singleton
@MetaInfServices(value = RSVPExtensionConsumerContext.class)
public class DefaultRSVPExtensionConsumerContext extends ForwardingRSVPExtensionConsumerContext {
    private final @NonNull SimpleRSVPExtensionProviderContext delegate = new SimpleRSVPExtensionProviderContext();

    public DefaultRSVPExtensionConsumerContext() {
        this(ServiceLoader.load(RSVPExtensionProviderActivator.class));
    }

    @VisibleForTesting
    public DefaultRSVPExtensionConsumerContext(final RSVPExtensionProviderActivator... extensionActivators) {
        this(Arrays.asList(extensionActivators));
    }

    @Inject
    public DefaultRSVPExtensionConsumerContext(final Iterable<RSVPExtensionProviderActivator> extensionActivators) {
        extensionActivators.forEach(activator -> activator.start(delegate));
    }

    @Override
    RSVPExtensionConsumerContext delegate() {
        return delegate;
    }
}
