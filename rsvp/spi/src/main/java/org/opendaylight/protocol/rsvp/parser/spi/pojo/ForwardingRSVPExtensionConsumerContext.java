/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.pojo;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.LabelRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionConsumerContext;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectRegistry;

abstract class ForwardingRSVPExtensionConsumerContext implements RSVPExtensionConsumerContext {

    abstract @NonNull RSVPExtensionConsumerContext delegate();

    @Override
    public final RSVPTeObjectRegistry getRsvpRegistry() {
        return delegate().getRsvpRegistry();
    }

    @Override
    public final XROSubobjectRegistry getXROSubobjectHandlerRegistry() {
        return delegate().getXROSubobjectHandlerRegistry();
    }

    @Override
    public final EROSubobjectRegistry getEROSubobjectHandlerRegistry() {
        return delegate().getEROSubobjectHandlerRegistry();
    }

    @Override
    public final RROSubobjectRegistry getRROSubobjectHandlerRegistry() {
        return delegate().getRROSubobjectHandlerRegistry();
    }

    @Override
    public final LabelRegistry getLabelHandlerRegistry() {
        return delegate().getLabelHandlerRegistry();
    }
}
