/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;

/**
 * Interface for registering AdjRIBsIn factories. In order for a model-driven RIB implementation to work correctly, it
 * has to know how to handle individual NLRI fields, whose encoding is specific to a AFI/SAFI pair. This interface
 * exposes an interface for registration of factories for creating AdjRIBsIn instances, which handle the specifics.
 */
public interface RIBExtensionProviderContext extends RIBExtensionConsumerContext {

    /**
     * Register a RIBSupport instance for a particular AFI/SAFI combination.
     *
     * @param afi     Address Family identifier
     * @param safi    Subsequent Address Family identifier
     * @param support T RIBSupport instance
     * @return Registration handle. Call {@link RIBSupportRegistration#close()} method to remove it.
     * @throws NullPointerException if any of the arguments is null
     */
    <T extends RIBSupport> RIBSupportRegistration<T> registerRIBSupport(
            @Nonnull Class<? extends AddressFamily> afi,
            @Nonnull Class<? extends SubsequentAddressFamily> safi, T support);
}
