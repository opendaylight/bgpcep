/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

/**
 * Interface for acquiring AdjRIBsIn factories. In order for a model-driven RIB implementation to work correctly, it
 * has to know how to handle individual NLRI fields, whose encoding is specific to a AFI/SAFI pair. This interface
 * exposes an entry point for locating the AFI/SAFI-specific implementation handler.
 */
public interface RIBExtensionConsumerContext {

    /**
     * Acquire a RIB implementation factory for a AFI/SAFI combination.
     *
     * @param key AFI/SAFI key
     * @return RIBSupport instance, or null if the AFI/SAFI is not implemented.
     */
    @Nullable
    RIBSupport getRIBSupport(@Nonnull TablesKey key);

    /**
     * Acquire a RIB implementation factory for a AFI/SAFI combination.
     *
     * @param afi  Address Family Identifier
     * @param safi Subsequent Address Family identifier
     * @return RIBSupport instance, or null if the AFI/SAFI is not implemented.
     */
    @Nullable
    RIBSupport getRIBSupport(@Nonnull Class<? extends AddressFamily> afi,
        @Nonnull Class<? extends SubsequentAddressFamily> safi);

    /**
     * Acquire a RIB implementation factory for a AFI/SAFI combination.
     *
     * @param key Tables key with AFI/SAFI
     * @return RIBSupport instance, or null if the AFI/SAFI is
     *     not implemented.
     */
    @Nullable
    RIBSupport getRIBSupport(@Nonnull NodeIdentifierWithPredicates key);


    /**
     * Returns class loading strategy for loading YANG modeled classes
     * associated with registered RIB supports.
     *
     * @return Class loading strategy for loading YANG modeled classes.
     */
    @Nonnull
    GeneratedClassLoadingStrategy getClassLoadingStrategy();
}