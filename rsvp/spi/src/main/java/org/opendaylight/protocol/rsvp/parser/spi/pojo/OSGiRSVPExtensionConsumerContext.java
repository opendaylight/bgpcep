/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.pojo;

import static com.google.common.base.Verify.verifyNotNull;

import java.util.List;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionConsumerContext;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionProviderActivator;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true, service = RSVPExtensionConsumerContext.class)
// FIXME: merge with DefaultRSVPExtensionConsumerContext once we have OSGi R7
public final class OSGiRSVPExtensionConsumerContext extends ForwardingRSVPExtensionConsumerContext {
    @Reference
    List<RSVPExtensionProviderActivator> extensionActivators;

    private SimpleRSVPExtensionProviderContext delegate;

    @Activate
    void activate() {
        final SimpleRSVPExtensionProviderContext local = new SimpleRSVPExtensionProviderContext();
        extensionActivators.forEach(activator -> activator.start(local));
        delegate = local;
    }

    @Deactivate
    void deactivate() {
        delegate = null;
    }

    @Override
    RSVPExtensionConsumerContext delegate() {
        return verifyNotNull(delegate);
    }
}
