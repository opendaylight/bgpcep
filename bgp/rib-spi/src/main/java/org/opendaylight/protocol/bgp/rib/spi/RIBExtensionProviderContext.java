/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Registration;

/**
 * Interface for registering AdjRIBsIn factories. In order for a model-driven RIB implementation to work correctly, it
 * has to know how to handle individual NLRI fields, whose encoding is specific to a AFI/SAFI pair. This interface
 * exposes an interface for registration of factories for creating AdjRIBsIn instances, which handle the specifics.
 */
public interface RIBExtensionProviderContext extends RIBExtensionConsumerContext {
    /**
     * Register a RIBSupport instance for a particular AFI/SAFI combination.
     *
     * @param support RIBSupport instance
     * @return Registration handle. Call {@link Registration#close()} method to remove it.
     * @throws NullPointerException if {@code support} is {@code null}
     */
    @NonNull Registration registerRIBSupport(RIBSupport<?, ?> support);
}
